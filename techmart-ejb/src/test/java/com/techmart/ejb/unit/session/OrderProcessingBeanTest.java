package com.techmart.ejb.unit.session;

import com.techmart.common.dto.CartItemDTO;
import com.techmart.common.dto.OrderDTO;
import com.techmart.common.entity.Customer;
import com.techmart.common.entity.Order;
import com.techmart.common.entity.OrderItem;
import com.techmart.common.entity.Product;
import com.techmart.common.enums.OrderStatus;
import com.techmart.common.enums.PaymentStatus;
import com.techmart.ejb.dao.OrderDAO;
import com.techmart.ejb.dao.OrderItemAllocationDAO;
import com.techmart.ejb.jndi.EnvironmentPropertyManager;
import com.techmart.ejb.jndi.JNDIConfigurationManager;
import com.techmart.ejb.session.singleton.InventoryManagerBean;
import com.techmart.ejb.session.stateless.OrderProcessingBean;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.Queue;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderProcessingBean
 * Validates checkout flow, order creation, and JMS integration.
 */
@ExtendWith(MockitoExtension.class)
class OrderProcessingBeanTest {

    @InjectMocks
    private OrderProcessingBean orderProcessingBean;

    @Mock
    private EntityManager entityManager;

    @Mock
    private OrderDAO orderDAO;

    @Mock
    private OrderItemAllocationDAO allocationDAO;

    @Mock
    private InventoryManagerBean inventoryManager;

    @Mock
    private JMSContext jmsContext;

    @Mock
    private Queue orderQueue;

    @Mock
    private JNDIConfigurationManager jndiManager;

    @Mock
    private EnvironmentPropertyManager envManager;

    private Customer testCustomer;
    private List<CartItemDTO> testCartItems;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer("John", "Doe", "john@techmart.com", "password", "+941234567","Colombo");
        testCustomer.setId(1L);

        testCartItems = new ArrayList<>();
        testCartItems.add(new CartItemDTO(
                1L, "Laptop", "SKU-001",
                BigDecimal.valueOf(150000), 1,
                "/img/laptop.jpg", "Electronics"
        ));
    }

    @Test
    void testProcessCheckoutCreatesOrder() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Laptop");
        product.setSku("SKU-001");
        product.setSellingPrice(BigDecimal.valueOf(150000));

        when(entityManager.find(Product.class, 1L)).thenReturn(product);

        when(orderDAO.create(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            order.setOrderNumber("ORD-TEST-001");
            order.setStatus(OrderStatus.PENDING);
            order.setPaymentStatus(PaymentStatus.PENDING);
            order.setCreatedAt(LocalDateTime.now());
            order.setSubtotal(BigDecimal.valueOf(150000));
            order.setTaxAmount(BigDecimal.ZERO);
            order.setShippingAmount(BigDecimal.ZERO);
            order.setTotalAmount(BigDecimal.valueOf(150000));
            return order;
        });

        JMSProducer mockProducer = mock(JMSProducer.class);
        when(jmsContext.createProducer()).thenReturn(mockProducer);

        when(mockProducer.send(any(jakarta.jms.Destination.class), any(java.io.Serializable.class)))
                .thenReturn(mockProducer);

        OrderDTO result = orderProcessingBean.processCheckout(
                testCustomer, testCartItems, "123 Main St"
        );

        assertNotNull(result);
        assertEquals("ORD-TEST-001", result.orderNumber());
        assertEquals(OrderStatus.PENDING, result.status());
        assertEquals(BigDecimal.valueOf(150000), result.totalAmount());
        assertEquals(1, result.items().size());

        verify(jmsContext).createProducer();
        verify(mockProducer, atLeastOnce()).send(any(jakarta.jms.Destination.class), any(java.io.Serializable.class));
    }

    @Test
    void testProcessCheckoutThrowsExceptionForEmptyCart() {
        assertThrows(IllegalArgumentException.class, () -> {
            orderProcessingBean.processCheckout(testCustomer, new ArrayList<>(), "123 Main St");
        });
    }

    @Test
    void testProcessCheckoutThrowsExceptionForInvalidProduct() {
        when(entityManager.find(Product.class, 1L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> {
            orderProcessingBean.processCheckout(testCustomer, testCartItems, "123 Main St");
        });
    }

    @Test
    void testUpdateOrderStatus() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNumber("ORD-TEST-001");
        order.setStatus(OrderStatus.PENDING);

        when(orderDAO.findById(1L)).thenReturn(order);

        orderProcessingBean.updateOrderStatus(1L, OrderStatus.CONFIRMED);

        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        verify(orderDAO).update(order);
    }

    @Test
    void testGetCustomerOrders() {
        List<Order> orders = new ArrayList<>();
        Order order = new Order();
        order.setId(1L);
        order.setOrderNumber("ORD-TEST-001");
        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentStatus(PaymentStatus.COMPLETED);
        order.setTotalAmount(BigDecimal.valueOf(150000));
        order.setSubtotal(BigDecimal.valueOf(150000));
        order.setTaxAmount(BigDecimal.ZERO);
        order.setShippingAmount(BigDecimal.ZERO);
        order.setCreatedAt(LocalDateTime.now());
        order.setCustomer(testCustomer);

        OrderItem orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setQuantity(1);
        orderItem.setSellingPrice(BigDecimal.valueOf(150000));
        orderItem.setSubtotal(BigDecimal.valueOf(150000));

        Product product = new Product();
        product.setId(1L);
        product.setName("Laptop");
        product.setSku("SKU-001");
        orderItem.setProduct(product);

        orderItem.setOrder(order);
        order.getItems().add(orderItem);

        orders.add(order);

        when(orderDAO.findByCustomer(1L, 0, 10)).thenReturn(orders);

        List<OrderDTO> result = orderProcessingBean.getCustomerOrders(1L, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ORD-TEST-001", result.get(0).orderNumber());
        assertEquals(1, result.get(0).items().size());
    }
}
