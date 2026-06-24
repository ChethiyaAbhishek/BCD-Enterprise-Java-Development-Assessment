package com.techmart.ejb.session.stateful;

import com.techmart.common.dto.CartItemDTO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.PostActivate;
import jakarta.ejb.PrePassivate;
import jakarta.ejb.Remove;
import jakarta.ejb.Stateful;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Stateful
@Log
public class ShoppingCartBean implements Serializable {

    @Getter
    private List<CartItemDTO> items;

    @Setter
    private Long customerId;

    @PostConstruct
    public void init(){
        items = new ArrayList<>();
        log.info("ShoppingCartBean created for new session.");
    }

    @PrePassivate
    public void prePassivate(){
        log.info("Passivating ShoppingCartBean. Current items: " + items.size());
    }

    @PostActivate
    public void postActivate(){
        log.info("ShoppingCartBean activated. Current items: " + items.size());
    }

    @PreDestroy
    public void preDestroy(){
        if(items!=null){
            items.clear();
        }
        log.info("Destroying ShoppingCartBean.");
    }

    public void addItem(CartItemDTO item){
        items.stream().filter(cartItem-> cartItem.productId().equals(item.productId()))
                .findFirst().ifPresentOrElse(existingItem->{
                    int newQty = existingItem.quantity() + item.quantity();
                    items.set(items.indexOf(existingItem), new CartItemDTO(
                            existingItem.productId(),
                            existingItem.productName(),
                            existingItem.productSku(),
                            existingItem.unitPrice(),
                            newQty, existingItem.imageUrl(),
                            existingItem.categoryName()));
                }, ()-> items.add(item));
        log.info("Cart item added.");
    }

    public void removeItem(Long productId){
        items.removeIf(item-> item.productId().equals(productId));
        log.info("Cart item removed.");
    }

    public void updateQty(Long productId, int qty){
        if(qty <= 0){
            removeItem(productId);
        }else{
            items.stream().filter(cartItem-> cartItem.productId().equals(productId))
                    .findFirst().ifPresent(item->{
                        items.set(items.indexOf(item), new CartItemDTO(
                                item.productId(),
                                item.productName(),
                                item.productSku(),
                                item.unitPrice(),
                                qty, item.imageUrl(),
                                item.categoryName()));
                    });
            log.info("Cart item qty updated.");
        }
    }

    public BigDecimal getSubTotal(){
        return items.stream()
                .map(CartItemDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Remove
    public void clearCart(){
        items.clear();
    }

}
