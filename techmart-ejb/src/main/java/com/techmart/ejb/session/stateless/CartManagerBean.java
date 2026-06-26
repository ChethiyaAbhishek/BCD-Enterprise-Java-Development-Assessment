package com.techmart.ejb.session.stateless;

import com.techmart.common.dto.CartItemDTO;
import com.techmart.ejb.session.stateful.ShoppingCartBean;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import lombok.extern.java.Log;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Stateless
@Log
public class CartManagerBean {

    private final Map<Long, ShoppingCartBean> customerCarts = new ConcurrentHashMap<>();

    @EJB
    private ShoppingCartBean shoppingCartBeanFactory;

    public ShoppingCartBean getOrCreateCart(Long customerId) {
        return customerCarts.computeIfAbsent(customerId, id -> {
            log.info("Creating new cart for customer: " + id);

            ShoppingCartBean newCart = shoppingCartBeanFactory;
            newCart.setCustomerId(id);
            return newCart;
        });
    }

    public ShoppingCartBean getCart(Long customerId) {
        return customerCarts.get(customerId);
    }

    public void removeCart(Long customerId) {
        ShoppingCartBean cart = customerCarts.remove(customerId);
        if (cart != null) {
            cart.clearCart();
            log.info("Cart removed for customer: " + customerId);
        }
    }

    public void addItem(Long customerId, CartItemDTO item) {
        ShoppingCartBean cart = getOrCreateCart(customerId);
        log.info("Cart Items Count: " + cart.getItems().size());
        cart.addItem(item);
    }

    public List<CartItemDTO> getCartItems(Long customerId) {
        ShoppingCartBean cart = customerCarts.get(customerId);
        return cart != null ? cart.getItems() : List.of();
    }

    public BigDecimal getCartSubtotal(Long customerId) {
        ShoppingCartBean cart = customerCarts.get(customerId);
        return cart != null ? cart.getSubTotal() : BigDecimal.ZERO;
    }
}