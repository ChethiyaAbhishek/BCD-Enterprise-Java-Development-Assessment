package com.techmart.ejb.session.stateless;

import com.techmart.common.dto.AllocationResult;
import com.techmart.common.dto.CartItemDTO;
import com.techmart.common.dto.NotificationDTO;
import com.techmart.common.dto.OrderDTO;
import com.techmart.common.entity.*;
import com.techmart.common.enums.NotificationType;
import com.techmart.common.enums.OrderStatus;
import com.techmart.common.enums.Priority;
import com.techmart.ejb.dao.OrderDAO;
import com.techmart.ejb.dao.OrderItemAllocationDAO;
import com.techmart.ejb.interceptor.PerformanceMonitored;
import com.techmart.ejb.jms.JMSConfigurationConstants;
import com.techmart.ejb.jndi.EnvironmentPropertyManager;
import com.techmart.ejb.jndi.JNDIConfigurationManager;
import com.techmart.ejb.session.singleton.InventoryManagerBean;
import jakarta.annotation.Resource;
import jakarta.ejb.*;
import jakarta.inject.Inject;
import jakarta.jms.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.java.Log;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
@PerformanceMonitored
@Log
public class OrderProcessingBean {

    @PersistenceContext(unitName = "techmartPU")
    private EntityManager entityManager;

    @Inject
    private OrderDAO orderDAO;

    @Inject
    private OrderItemAllocationDAO allocationDAO;

    @Inject
    private InventoryManagerBean inventoryManager;

    @Inject
    private JMSContext jmsContext;

    @Resource(lookup = JMSConfigurationConstants.ORDER_QUEUE)
    private Queue orderQueue;

    @Resource(lookup = JMSConfigurationConstants.NOTIFICATION_TOPIC)
    private Topic notificationTopic;

    @Inject
    private JNDIConfigurationManager jndiManager;

    @Inject
    private EnvironmentPropertyManager envManager;

    /**
     * Processes checkout: Creates order, reserves inventory, creates allocations,
     * sends to JMS for async processing
     */
    @PerformanceMonitored(value = "OrderProcessing.processCheckout", slowThresholdMs = 500)
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public OrderDTO processCheckout(Customer customer, @NonNull List<CartItemDTO> cartItems, String shippingAddress) {

        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        Order order = new Order();
        order.setCustomer(customer);
        order.setShippingAddress(shippingAddress);

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItemDTO cartItem : cartItems) {
            Product product = entityManager.find(Product.class, cartItem.productId());

            if (product == null) {
                throw new IllegalArgumentException("Product with ID " + cartItem.productId() + " no longer exists.");
            }

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(cartItem.quantity());
            item.setSellingPrice(product.getSellingPrice());
            item.setSubtotal(product.getSellingPrice().multiply(BigDecimal.valueOf(cartItem.quantity())));

            order.getItems().add(item);
            orderItems.add(item);
        }

        order.recalculateTotals();
        orderDAO.create(order);
        entityManager.flush();

        List<AllocationResult> allocations = inventoryManager.allocateOrderItems(orderItems);

        Map<Long, List<AllocationResult>> grouped = allocations.stream()
                .collect(Collectors.groupingBy(a -> a.getOrderItem().getId()));

        for (OrderItem item : orderItems) {
            List<AllocationResult> itemAllocs = grouped.get(item.getId());
            if (itemAllocs != null) {
                for (AllocationResult result : itemAllocs) {
                    OrderItemAllocation alloc = new OrderItemAllocation();
                    alloc.setOrderItem(item);
                    alloc.setBatch(entityManager.find(InventoryBatch.class, result.getBatch().getId()));
                    alloc.setQuantity(result.getQuantity());
                    alloc.setCostPrice(result.getCostPrice());
                    item.getAllocations().add(alloc);
                    allocationDAO.create(alloc);
                }
            }
        }

        sendOrderToQueue(order);
        sendAdminAlertIfHighValue(order);
        return mapToOrderDTO(order);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void confirmOrderAndNotify(Long orderId) {
        log.info("Confirming order and dispatching notifications for Order ID: " + orderId);

        Order order = orderDAO.findById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }

        order.setStatus(OrderStatus.CONFIRMED);
        orderDAO.update(order);

        log.info("Order " + order.getOrderNumber() + " status updated to CONFIRMED in database.");

        try {
            NotificationDTO notification = new NotificationDTO(
                    null,
                    order.getCustomer().getId(),
                    order.getCustomer().getEmail(),
                    order.getCustomer().getPhone(),
                    order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName(),
                    order.getId(),
                    order.getOrderNumber(),
                    NotificationType.ORDER_CONFIRMATION,
                    Priority.HIGH,
                    "Order Confirmation: " + order.getOrderNumber(),
                    "Thank you for your purchase! Your order is being processed.",
                    LocalDateTime.now()
            );

            jmsContext.createProducer()
                    .setDeliveryMode(DeliveryMode.PERSISTENT)
                    .send(notificationTopic, notification);

            log.info("Dispatched confirmation notification for Order " + order.getOrderNumber());
        } catch (JMSRuntimeException e) {
            log.severe("Failed to send notification: " + e.getMessage());
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderDAO.findById(orderId);
        if (order == null) throw new IllegalArgumentException("Order not found: " + orderId);

        order.setStatus(newStatus);

        orderDAO.update(order);
        log.info("Order " + order.getOrderNumber() + " status updated to " + newStatus);
    }

    /**
     * Get order history for a customer (paginated for performance)
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<OrderDTO> getCustomerOrders(Long customerId, int page, int pageSize) {
        List<Order> orders = orderDAO.findByCustomer(customerId, page * pageSize, pageSize);
        return orders.stream().map(this::mapToOrderDTO).collect(Collectors.toList());
    }

    private void sendAdminAlertIfHighValue(Order order) {
        BigDecimal highValueThreshold = new BigDecimal("100000");

        if (order.getTotalAmount().compareTo(highValueThreshold) > 0) {
            log.info("High-value order detected: " + order.getOrderNumber() + ". Triggering admin alert.");

            String adminQueueJndi = envManager.getAdminAlertQueueJndi();

            Queue adminQueue = jndiManager.lookupDynamicResource(adminQueueJndi, Queue.class);

            if (adminQueue != null) {
                try {
                    String alertMessage = "ALERT: High-value order " + order.getOrderNumber() +
                            " placed. Total: " + order.getTotalAmount();

                    jmsContext.createProducer()
                            .setDeliveryMode(DeliveryMode.PERSISTENT)
                            .setPriority(9)
                            .send(adminQueue, alertMessage);

                    log.info("Successfully sent admin alert to dynamic queue: " + adminQueueJndi);
                } catch (JMSRuntimeException e) {
                    log.severe("Failed to send admin alert to " + adminQueueJndi + ": " + e.getMessage());
                }
            } else {
                log.warning("Admin alert queue not found at JNDI: " + adminQueueJndi);
            }
        }
    }


    private void sendOrderToQueue(Order order) {
        jmsContext.createProducer().send(orderQueue, mapToOrderDTO(order));
    }

    private OrderDTO mapToOrderDTO(Order order) {
        List<OrderDTO.OrderItemDTO> itemDTOs = order.getItems().stream()
                .map(this::mapToOrderItemDTO)
                .collect(Collectors.toList());

        return new OrderDTO(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomer().getId(),
                order.getCustomer().getEmail(),
                order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName(),
                itemDTOs,
                order.getSubtotal(),
                order.getTaxAmount(),
                order.getShippingAmount(),
                order.getTotalAmount(),
                order.getShippingAddress(),
                order.getBillingAddress(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getCreatedAt()
        );
    }

    private OrderDTO.OrderItemDTO mapToOrderItemDTO(OrderItem item) {
        List<OrderDTO.AllocationDTO> allocationDTOs = item.getAllocations().stream()
                .map(this::mapToAllocationDTO)
                .collect(Collectors.toList());

        return new OrderDTO.OrderItemDTO(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getSku(),
                item.getQuantity(),
                item.getSellingPrice(),
                item.getSubtotal(),
                allocationDTOs
        );
    }

    private OrderDTO.AllocationDTO mapToAllocationDTO(OrderItemAllocation alloc) {
        InventoryBatch batch = alloc.getBatch();
        Inventory inventory = batch.getInventory();
        Warehouse warehouse = inventory.getWarehouse();

        return new OrderDTO.AllocationDTO(
                batch.getId(),
                batch.getBatchNumber(),
                warehouse.getId(),
                warehouse.getCode(),
                alloc.getQuantity(),
                alloc.getCostPrice()
        );
    }
}
