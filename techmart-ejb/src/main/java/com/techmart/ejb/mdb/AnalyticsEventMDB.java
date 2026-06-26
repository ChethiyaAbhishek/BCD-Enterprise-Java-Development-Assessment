package com.techmart.ejb.mdb;

import com.techmart.ejb.mdb.base.BaseMessageProcessor;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import lombok.extern.java.Log;

@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup",
                propertyValue = "jms/AnalyticsTopic"),
        @ActivationConfigProperty(propertyName = "destinationType",
                propertyValue = "jakarta.jms.Topic"),
        @ActivationConfigProperty(propertyName = "subscriptionDurability",
                propertyValue = "NonDurable"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode",
                propertyValue = "Auto-acknowledge")
})
@Log
public class AnalyticsEventMDB extends BaseMessageProcessor<String> {

    @Override
    protected String extractPayload(Message message) throws JMSException {
        if (message instanceof TextMessage textMsg) {
            return textMsg.getText();
        }
        throw new JMSException("Invalid message type for AnalyticsTopic. Expected TextMessage.");
    }

    @Override
    protected void processMessage(String eventPayload) throws Exception {
        // eventType|userId|details
        String[] parts = eventPayload.split("\\|");
        if (parts.length == 3) {
            log.fine("Analytics Event: Type=" + parts[0] + ", User=" + parts[1] + ", Details=" + parts[2]);
        }
    }
}
