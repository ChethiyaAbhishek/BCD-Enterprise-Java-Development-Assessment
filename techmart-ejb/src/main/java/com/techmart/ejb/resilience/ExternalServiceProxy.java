package com.techmart.ejb.resilience;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import lombok.extern.java.Log;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

@Singleton
@Startup
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@Log
public class ExternalServiceProxy {

    private CircuitBreaker paymentGatewayCircuit;
    private CircuitBreaker deliveryPartnerCircuit;
    private CircuitBreaker emailGatewayCircuit;

    @PostConstruct
    public void init() {
        paymentGatewayCircuit = new CircuitBreaker("PaymentGateway", 5, 30000, 3);
        deliveryPartnerCircuit = new CircuitBreaker("DeliveryPartner", 3, 60000, 2);
        emailGatewayCircuit = new CircuitBreaker("EmailGateway", 10, 15000, 5);

        log.info("ExternalServiceProxy initialized with circuit breakers.");
    }

    public <T> T executeWithCircuitBreaker(String serviceName, Callable<T> serviceCall, Supplier<T> fallback) {
        CircuitBreaker circuit = getCircuitBreaker(serviceName);

        if (!circuit.allowRequest()) {
            log.warning("CircuitBreaker [" + serviceName + "] is OPEN. Returning fallback response.");
            return fallback.get();
        }

        try {
            T result = serviceCall.call();
            circuit.recordSuccess();
            return result;

        } catch (Exception e) {
            circuit.recordFailure();
            log.warning("External service [" + serviceName + "] failed: " + e.getMessage());

            log.info("Returning fallback response for [" + serviceName + "] due to service failure.");
            return fallback.get();
        }
    }

    public void executeVoidWithCircuitBreaker(String serviceName, Runnable serviceCall, Runnable fallback) {
        CircuitBreaker circuit = getCircuitBreaker(serviceName);

        if (!circuit.allowRequest()) {
            log.warning("CircuitBreaker [" + serviceName + "] is OPEN. Executing fallback.");
            fallback.run();
            return;
        }

        try {
            serviceCall.run();
            circuit.recordSuccess();
        } catch (Exception e) {
            circuit.recordFailure();
            log.warning("External service [" + serviceName + "] failed: " + e.getMessage() + ". Executing fallback.");
            fallback.run();
        }
    }

    private CircuitBreaker getCircuitBreaker(String serviceName) {
        return switch (serviceName) {
            case "PaymentGateway" -> paymentGatewayCircuit;
            case "DeliveryPartner" -> deliveryPartnerCircuit;
            case "EmailGateway" -> emailGatewayCircuit;
            default -> throw new IllegalArgumentException("Unknown service: " + serviceName);
        };
    }

    public CircuitBreaker.State getCircuitState(String serviceName) {
        return getCircuitBreaker(serviceName).getState();
    }

    public String getCircuitMetrics(String serviceName) {
        CircuitBreaker cb = getCircuitBreaker(serviceName);
        return String.format("Service: %s | State: %s | Requests: %d | Failures: %d | Rejected: %d",
                serviceName, cb.getState(), cb.getTotalRequests(), cb.getTotalFailures(), cb.getTotalRejected());
    }
}