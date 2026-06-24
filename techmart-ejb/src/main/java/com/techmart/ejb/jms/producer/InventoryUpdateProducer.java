package com.techmart.ejb.jms.producer;

import com.techmart.common.dto.InventoryUpdateDTO;
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
public class InventoryUpdateProducer {

    @Inject
    private JMSContext jmsContext;

    @Resource(lookup = JMSConfigurationConstants.INVENTORY_QUEUE)
    private Queue inventoryQueue;

    public void sendInventoryUpdate(InventoryUpdateDTO update) {
        try {
            jmsContext.createProducer()
                    .setDeliveryMode(DeliveryMode.PERSISTENT)
                    .setPriority(5)
                    .send(inventoryQueue, update);

            log.info("Inventory update for product " + update.productId() + " sent to InventoryQueue.");
        } catch (JMSRuntimeException e) {
            log.severe("Failed to send inventory update: " + e.getMessage());
        }
    }
}
