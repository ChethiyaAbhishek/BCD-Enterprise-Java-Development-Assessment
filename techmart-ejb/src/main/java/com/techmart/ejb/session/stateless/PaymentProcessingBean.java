package com.techmart.ejb.session.stateless;

import com.techmart.common.entity.Order;
import com.techmart.common.enums.OrderStatus;
import com.techmart.common.enums.PaymentStatus;
import com.techmart.ejb.async.PaymentGatewayService;
import com.techmart.ejb.dao.OrderDAO;
import jakarta.ejb.*;
import jakarta.inject.Inject;
import lombok.extern.java.Log;

import java.math.BigDecimal;

@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
@Log
public class PaymentProcessingBean {

    @Inject
    private OrderDAO orderDAO;

    @Inject
    private PaymentGatewayService paymentGateway;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public OrderStatus initiatePayment(Long orderId, BigDecimal amount, String cardToken) {
        Order order = orderDAO.findById(orderId);
        if (order == null) throw new IllegalArgumentException("Order not found");

        order.setStatus(OrderStatus.PROCESSING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        orderDAO.update(order);

        paymentGateway.processPaymentAsync(orderId, amount, cardToken);

        return OrderStatus.PROCESSING;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void handlePaymentSuccess(Long orderId, String transactionId) {
        Order order = orderDAO.findById(orderId);
        if (order == null) return;

        order.setPaymentStatus(PaymentStatus.COMPLETED);
        order.setPaymentTransactionId(transactionId);
        order.setStatus(OrderStatus.CONFIRMED);
        orderDAO.update(order);

        log.info("Payment successful for Order " + orderId + " (TXN: " + transactionId + ")");
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void handlePaymentFailure(Long orderId, String errorMessage) {
        Order order = orderDAO.findById(orderId);
        if (order == null) return;

        order.setPaymentStatus(PaymentStatus.FAILED);
        order.setStatus(OrderStatus.CANCELLED);
        order.setNotes("Payment failed: " + errorMessage);
        orderDAO.update(order);

        log.warning("Payment failed for Order " + orderId + ": " + errorMessage);
    }
}