package com.techmart.ejb.mdb;

import com.techmart.common.dto.InventoryUpdateDTO;
import com.techmart.ejb.mdb.base.BaseMessageProcessor;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.ObjectMessage;
import lombok.extern.java.Log;

@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup",
                propertyValue = "jms/InventoryQueue"),
        @ActivationConfigProperty(propertyName = "destinationType",
                propertyValue = "jakarta.jms.Queue"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode",
                propertyValue = "Auto-acknowledge")
})
@Log
public class InventorySyncMDB extends BaseMessageProcessor<InventoryUpdateDTO> {

    @Override
    protected InventoryUpdateDTO extractPayload(Message message) throws JMSException {
        if (message instanceof ObjectMessage objMsg) {
            return (InventoryUpdateDTO) objMsg.getObject();
        }
        throw new JMSException("Invalid message type for InventoryQueue.");
    }

    @Override
    protected void processMessage(InventoryUpdateDTO update) throws Exception {
        log.info("Syncing inventory for Product " + update.productId() +
                " at Warehouse " + update.warehouseId() + ". Change: " + update.changeAmount());

        // Simulate external ERP API call
        Thread.sleep(100);

        log.info("ERP sync successful for batch " + update.batchNumber());
    }
}
