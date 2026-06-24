package com.techmart.ejb.session.stateless;

import com.techmart.common.dto.AllocationResult;
import com.techmart.common.dto.CartItemDTO;
import com.techmart.common.dto.OrderDTO;
import com.techmart.common.entity.*;
import com.techmart.common.enums.OrderStatus;
import com.techmart.ejb.dao.OrderDAO;
import com.techmart.ejb.dao.OrderItemAllocationDAO;
import com.techmart.ejb.jms.JMSConfigurationConstants;
import com.techmart.ejb.session.singleton.InventoryManagerBean;
import jakarta.annotation.Resource;
import jakarta.ejb.*;
import jakarta.inject.Inject;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.java.Log;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
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

    /**
     * Processes checkout: Creates order, reserves inventory, creates allocations,
     * sends to JMS for async processing
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public OrderDTO processCheckout(Customer customer, @NonNull List<CartItemDTO> cartItems, String shippingAddress) {

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
        return mapToOrderDTO(order);
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
     * Get order history for a customer (paginated for performance).
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<OrderDTO> getCustomerOrders(Long customerId, int page, int pageSize) {
        List<Order> orders = orderDAO.findByCustomer(customerId, page * pageSize, pageSize);
        return orders.stream().map(this::mapToOrderDTO).collect(Collectors.toList());
    }


    private void sendOrderToQueue(Order order) {
        jmsContext.createProducer().send(orderQueue, mapToOrderDTO(order));
    }

    private OrderDTO mapToOrderDTO(Order order) {
        return new OrderDTO(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomer().getId(),
                order.getCustomer().getEmail(),
                order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName(),
                new ArrayList<>(),
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
}
