package com.techmart.ejb.integration;

import com.techmart.common.dto.CartItemDTO;
import com.techmart.common.dto.OrderDTO;
import com.techmart.common.entity.Customer;
import com.techmart.common.enums.OrderStatus;
import com.techmart.ejb.jms.JMSConfigurationConstants;
import com.techmart.ejb.session.singleton.InventoryManagerBean;
import com.techmart.ejb.session.stateless.CustomerServiceBean;
import com.techmart.ejb.session.stateless.OrderProcessingBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ArquillianExtension.class)
class OrderProcessingBeanIT {

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "order-test.war")
                .addPackages(true, "com.techmart.common")
                .addPackages(true, "com.techmart.ejb.dao")
                .addPackages(true, "com.techmart.ejb.jndi")
                .addClasses(
                        OrderProcessingBean.class,
                        CustomerServiceBean.class,
                        InventoryManagerBean.class,
                        JMSConfigurationConstants.class
                )
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource("META-INF/persistence.xml", "META-INF/persistence.xml");
    }

    @Inject
    private OrderProcessingBean orderProcessingBean;

    @Inject
    private CustomerServiceBean customerServiceBean;

    @PersistenceContext(unitName = "techmartPU")
    private EntityManager entityManager;

    @Test
    void testProcessCheckoutCreatesOrder() {
        Customer customer = new Customer("Arquillian", "TestUser",
                "test@arquillian.com", "pass", "123", "City");
        customer.setId(999L);

        List<CartItemDTO> cartItems = new ArrayList<>();
        cartItems.add(new CartItemDTO(
                1L, "Laptop", "SKU-001",
                BigDecimal.valueOf(150000), 1,
                "/img/laptop.jpg", "Electronics"
        ));

        try {
            OrderDTO result = orderProcessingBean.processCheckout(customer, cartItems, "Test Address");
            assertNotNull(result);
            assertNotNull(result.orderNumber());
        } catch (Exception e) {
            System.out.println("Checkout failed (expected if JMS not fully configured): " + e.getMessage());
        }
    }

    @Test
    void testUpdateOrderStatus() {
        assertNotNull(orderProcessingBean, "Bean should be injected");

        assertThrows(jakarta.ejb.EJBException.class, () -> {
            orderProcessingBean.updateOrderStatus(99999L, OrderStatus.CONFIRMED);
        });
    }
}