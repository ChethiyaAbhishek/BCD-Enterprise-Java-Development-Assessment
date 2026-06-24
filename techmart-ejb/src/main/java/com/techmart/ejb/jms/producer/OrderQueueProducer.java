package com.techmart.ejb.jms.producer;

import com.techmart.common.dto.OrderDTO;
import com.techmart.ejb.jms.JMSConfigurationConstants;
import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.jms.DeliveryMode;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSRuntimeException;
import jakarta.jms.Queue;
import lombok.extern.java.Log;

@Stateless
@Log
public class OrderQueueProducer {

    @Inject
    private JMSContext jmsContext;

    @Resource(lookup = JMSConfigurationConstants.ORDER_QUEUE)
    private Queue orderQueue;

    public void sendOrder(OrderDTO order) {
        try {
            jmsContext.createProducer()
                    .setDeliveryMode(DeliveryMode.PERSISTENT)
                    .setPriority(9)
                    .send(orderQueue, order);

            log.info("Order " + order.orderNumber() + " sent to OrderQueue successfully.");
        } catch (JMSRuntimeException e) {
            log.severe("Failed to send order " + order.orderNumber() + " to JMS: " + e.getMessage());
        }
    }
}
