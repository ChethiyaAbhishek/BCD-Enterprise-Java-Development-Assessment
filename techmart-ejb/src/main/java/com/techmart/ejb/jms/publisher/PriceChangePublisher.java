package com.techmart.ejb.jms.publisher;

import com.techmart.ejb.jms.JMSConfigurationConstants;
import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.jms.DeliveryMode;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSRuntimeException;
import jakarta.jms.Topic;
import lombok.extern.java.Log;

import java.math.BigDecimal;

@Stateless
@Log
public class PriceChangePublisher {

    @Inject
    private JMSContext jmsContext;

    @Resource(lookup = JMSConfigurationConstants.PRICE_CHANGE_TOPIC)
    private Topic priceChangeTopic;

    public void publishPriceChange(Long productId, String productName, BigDecimal newPrice) {
        try {
            String messageText = "Product: " + productName + " (ID: " + productId + ") updated to: " + newPrice;

            jmsContext.createProducer()
                    .setDeliveryMode(DeliveryMode.NON_PERSISTENT)
                    .setPriority(4)
                    .send(priceChangeTopic, messageText);

            log.info("Price change broadcasted for product " + productId);
        } catch (JMSRuntimeException e) {
            log.severe("Failed to broadcast price change: " + e.getMessage());
        }
    }
}
