package com.techmart.ejb.integration;

import com.techmart.common.dto.CartItemDTO;
import com.techmart.ejb.session.stateful.ShoppingCartBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ArquillianExtension.class)
class ShoppingCartBeanIT {

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "cart-test.war")
                .addClasses(
                        ShoppingCartBean.class,
                        CartItemDTO.class
                )
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    private ShoppingCartBean cartBean;

    @Test
    void testConversationalStateMaintenance() {
        assertNotNull(cartBean, "ShoppingCartBean should be injected");
        assertEquals(0, cartBean.getItems().size(), "Cart should start empty");

        CartItemDTO laptop = new CartItemDTO(
                1L, "Laptop", "SKU-001",
                BigDecimal.valueOf(150000), 1,
                "/img/laptop.jpg", "Electronics"
        );
        cartBean.addItem(laptop);

        assertEquals(1, cartBean.getItems().size(), "Cart should have 1 item after first add");
        assertEquals(BigDecimal.valueOf(150000), cartBean.getSubTotal());

        CartItemDTO mouse = new CartItemDTO(
                2L, "Mouse", "SKU-002",
                BigDecimal.valueOf(2500), 2,
                "/img/mouse.jpg", "Electronics"
        );
        cartBean.addItem(mouse);

        assertEquals(2, cartBean.getItems().size(), "Cart should have 2 items (state maintained)");

        BigDecimal expectedTotal = BigDecimal.valueOf(150000).add(BigDecimal.valueOf(5000));
        assertEquals(expectedTotal, cartBean.getSubTotal(), "Subtotal should reflect both items");

        cartBean.addItem(laptop);

        assertEquals(2, cartBean.getItems().size(), "Should still have 2 distinct items");

        long laptopQty = cartBean.getItems().stream()
                .filter(i -> i.productId().equals(1L))
                .findFirst()
                .get()
                .quantity();

        assertEquals(2, laptopQty, "Laptop quantity should be 2 after adding again");
    }
}