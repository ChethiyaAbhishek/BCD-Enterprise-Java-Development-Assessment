package com.techmart.ejb.unit.session;

import com.techmart.common.dto.CartItemDTO;
import com.techmart.ejb.session.stateful.ShoppingCartBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ShoppingCartBean (Stateful Session Bean)
 * Validates cart operations and state management.
 */
class ShoppingCartBeanTest {

    private ShoppingCartBean cartBean;

    @BeforeEach
    void setUp() {
        cartBean = new ShoppingCartBean();
        cartBean.init();
    }

    @Test
    void testAddItemToEmptyCart() {
        CartItemDTO item = new CartItemDTO(
                1L, "Laptop", "SKU-001",
                BigDecimal.valueOf(150000), 1,
                "/img/laptop.jpg", "Electronics"
        );

        cartBean.addItem(item);

        assertEquals(1, cartBean.getItems().size());
        assertEquals(BigDecimal.valueOf(150000), cartBean.getSubTotal());
    }

    @Test
    void testAddSameItemIncreasesQuantity() {
        CartItemDTO item1 = new CartItemDTO(
                1L, "Laptop", "SKU-001",
                BigDecimal.valueOf(150000), 1,
                "/img/laptop.jpg", "Electronics"
        );
        CartItemDTO item2 = new CartItemDTO(
                1L, "Laptop", "SKU-001",
                BigDecimal.valueOf(150000), 2,
                "/img/laptop.jpg", "Electronics"
        );

        cartBean.addItem(item1);
        cartBean.addItem(item2);

        assertEquals(1, cartBean.getItems().size());
        assertEquals(3, cartBean.getItems().get(0).quantity());
        assertEquals(BigDecimal.valueOf(450000), cartBean.getSubTotal());
    }

    @Test
    void testRemoveItem() {
        CartItemDTO item = new CartItemDTO(
                1L, "Laptop", "SKU-001",
                BigDecimal.valueOf(150000), 1,
                "/img/laptop.jpg", "Electronics"
        );

        cartBean.addItem(item);
        cartBean.removeItem(1L);

        assertEquals(0, cartBean.getItems().size());
        assertEquals(BigDecimal.ZERO, cartBean.getSubTotal());
    }

    @Test
    void testUpdateQuantity() {
        CartItemDTO item = new CartItemDTO(
                1L, "Laptop", "SKU-001",
                BigDecimal.valueOf(150000), 1,
                "/img/laptop.jpg", "Electronics"
        );

        cartBean.addItem(item);
        cartBean.updateQty(1L, 3);

        assertEquals(3, cartBean.getItems().get(0).quantity());
        assertEquals(BigDecimal.valueOf(450000), cartBean.getSubTotal());
    }

    @Test
    void testUpdateQuantityToZeroRemovesItem() {
        CartItemDTO item = new CartItemDTO(
                1L, "Laptop", "SKU-001",
                BigDecimal.valueOf(150000), 1,
                "/img/laptop.jpg", "Electronics"
        );

        cartBean.addItem(item);
        cartBean.updateQty(1L, 0);

        assertEquals(0, cartBean.getItems().size());
    }

    @Test
    void testClearCart() {
        cartBean.addItem(new CartItemDTO(
                1L, "Laptop", "SKU-001",
                BigDecimal.valueOf(150000), 1,
                "/img/laptop.jpg", "Electronics"
        ));

        cartBean.clearCart();

        assertEquals(0, cartBean.getItems().size());
    }

    @Test
    void testMultipleDifferentItems() {
        cartBean.addItem(new CartItemDTO(
                1L, "Laptop", "SKU-001",
                BigDecimal.valueOf(150000), 1,
                "/img/laptop.jpg", "Electronics"
        ));
        cartBean.addItem(new CartItemDTO(
                2L, "Mouse", "SKU-002",
                BigDecimal.valueOf(2500), 2,
                "/img/mouse.jpg", "Electronics"
        ));

        assertEquals(2, cartBean.getItems().size());
        assertEquals(BigDecimal.valueOf(155000), cartBean.getSubTotal());
    }
}
