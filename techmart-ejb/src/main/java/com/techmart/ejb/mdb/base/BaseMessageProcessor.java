package com.techmart.ejb.mdb.base;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import lombok.extern.java.Log;

import java.io.Serializable;

@Log
public abstract class BaseMessageProcessor<T extends Serializable> implements MessageListener {

    /**
     * TEMPLATE METHOD
     */
    @Override
    public void onMessage(Message message) {
        long startTime = System.nanoTime();
        String messageId = getMessageId(message);
        String destination = getDestination(message);

        log.info(() -> "Received message [" + messageId + "] on destination: " + destination);

        try {
            T payload = extractPayload(message);

            if (payload == null) {
                log.warning(() -> "Message [" + messageId + "] contained null payload. Ignoring.");
                return;
            }

            processMessage(payload);

            onSuccess(message, payload);
            log.info(() -> "Successfully processed message [" + messageId + "]");

        } catch (Exception e) {
            log.severe(() -> "Failed to process message [" + messageId + "]: " + e.getMessage());
            onError(message, e);
        } finally {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            recordMetrics(destination, durationMs);
        }
    }

    /**
     * Core business logic for processing the message
     */
    protected abstract void processMessage(T payload) throws Exception;

    protected abstract T extractPayload(Message message) throws JMSException;

    /**
     * called after successful processing
     */
    protected void onSuccess(Message message, T payload) {
    }

    /**
     * called when an exception occurs
     */
    protected void onError(Message message, Exception e) {
        try {
            int redeliveryCount = message.getIntProperty("JMSXDeliveryCount");
            if (redeliveryCount > 3) {
                log.severe(() -> "Message [" + getMessageId(message) +
                        "] exceeded max retries (" + redeliveryCount + ")");
            } else {
                log.warning(() -> "Message [" + getMessageId(message) +
                        "] will be redelivered. Attempt: " + redeliveryCount);
            }
        } catch (JMSException ex) {
            log.severe(() -> "Failed to check redelivery count: " + ex.getMessage());
        }
    }


    private String getMessageId(Message message) {
        try {
            return message.getJMSMessageID();
        } catch (JMSException e) {
            return "UNKNOWN_ID";
        }
    }

    private String getDestination(Message message) {
        try {
            return message.getJMSDestination().toString();
        } catch (JMSException e) {
            return "UNKNOWN_DESTINATION";
        }
    }

    private void recordMetrics(String destination, long durationMs) {
        log.fine(() -> "Message processing took " + durationMs + "ms on " + destination);
    }
}
