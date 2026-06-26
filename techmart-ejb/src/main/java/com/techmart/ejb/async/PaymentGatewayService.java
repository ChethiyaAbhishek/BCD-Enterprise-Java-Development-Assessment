package com.techmart.ejb.async;

import com.techmart.ejb.resilience.ExternalServiceProxy;
import com.techmart.ejb.session.stateless.PaymentProcessingBean;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import lombok.extern.java.Log;

import java.math.BigDecimal;

@Stateless
@Log
public class PaymentGatewayService {

    @Inject
    private PaymentProcessingBean paymentProcessingBean;

    @Inject
    private ExternalServiceProxy externalServiceProxy;

    @Asynchronous
    public void processPaymentAsync(Long orderId, BigDecimal amount, String cardToken) {
        try {
            log.info("Background thread processing payment for Order " + orderId);

            // Simulate external gateway latency (2 to 5 seconds)
            long simulatedLatency = 2000 + (long)(Math.random() * 3000);
            Thread.sleep(simulatedLatency);

            PaymentResult result = externalServiceProxy.executeWithCircuitBreaker(
                    "PaymentGateway",
                    () -> {

                        if (Math.random() > 0.1) { // 90% success
                            String txnId = "TXN-" + System.currentTimeMillis();
                            return new PaymentResult(true, txnId, "Success");
                        } else {
                            throw new RuntimeException("Gateway declined transaction");
                        }
                    },
                    () -> {
                        log.warning("Payment gateway circuit is OPEN. Using fallback response.");
                        return new PaymentResult(false, null, "Payment service temporarily unavailable");
                    }
            );

            if (result.success()) {
                paymentProcessingBean.handlePaymentSuccess(orderId, result.transactionId());
            } else {
                paymentProcessingBean.handlePaymentFailure(orderId, result.message());
            }


        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            paymentProcessingBean.handlePaymentFailure(orderId, "Thread interrupted");
        } catch (Exception e) {
            log.severe("Payment gateway error: " + e.getMessage());
            paymentProcessingBean.handlePaymentFailure(orderId, e.getMessage());
        }
    }

    public record PaymentResult(boolean success, String transactionId, String message) {}
}
