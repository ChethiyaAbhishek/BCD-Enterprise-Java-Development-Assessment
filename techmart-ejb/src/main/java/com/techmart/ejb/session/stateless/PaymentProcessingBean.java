package com.techmart.ejb.session.stateless;

import com.techmart.common.entity.Order;
import com.techmart.common.enums.PaymentStatus;
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

//    @Inject
//    private PaymentGatewayService paymentGateway; // @Asynchronous service

    /**
     * Initiate payment processing
     * PERFORMANCE: Returns immediately to user, processes asynchronously
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void initiatePayment(Long orderId, BigDecimal amount, String cardToken) {
        Order order = orderDAO.findById(orderId);
        if (order == null) throw new IllegalArgumentException("Order not found: " + orderId);

        if (order.getPaymentStatus() == PaymentStatus.COMPLETED) {
            log.warning("Payment already completed for order: " + orderId);
            return;
        }

        order.setPaymentStatus(PaymentStatus.PENDING);
        orderDAO.update(order);

//        paymentGateway.processPaymentAsync(orderId, amount, cardToken);
        log.info("Payment initiated for order: " + orderId);
    }

    /**
     * Handle payment success callback
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void handlePaymentSuccess(Long orderId, String transactionId) {
        Order order = orderDAO.findById(orderId);
        if (order == null) throw new IllegalArgumentException("Order not found: " + orderId);

        order.setPaymentStatus(PaymentStatus.COMPLETED);
        order.setPaymentTransactionId(transactionId);
        orderDAO.update(order);

        log.info("Payment confirmed for order: " + orderId + " TXN: " + transactionId);
    }

    /**
     * Handle payment failure.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void handlePaymentFailure(Long orderId, String errorMessage) {
        Order order = orderDAO.findById(orderId);
        if (order == null) throw new IllegalArgumentException("Order not found: " + orderId);

        order.setPaymentStatus(PaymentStatus.FAILED);
        order.setNotes(order.getNotes() + " Payment failed: " + errorMessage);
        orderDAO.update(order);

        log.warning("Payment failed for order: " + orderId + " - " + errorMessage);
    }

    /**
     * Process refund.
     */
//    @TransactionAttribute(TransactionAttributeType.REQUIRED)
//    public void processRefund(Long orderId) {
//        Order order = orderDAO.findById(orderId);
//        if (order == null || order.getPaymentStatus() != PaymentStatus.COMPLETED) {
//            throw new IllegalArgumentException("Order cannot be refunded");
//        }
//
//        order.setPaymentStatus(PaymentStatus.REFUNDED);
//        orderDAO.update(order);
//        log.info("Refund processed for order: " + orderId);
//    }
}