package com.techmart.ejb.mdb;

import com.techmart.common.dto.NotificationDTO;
import com.techmart.ejb.mdb.base.BaseMessageProcessor;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.ObjectMessage;
import lombok.extern.java.Log;

@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup",
                propertyValue = "jms/NotificationTopic"),
        @ActivationConfigProperty(propertyName = "destinationType",
                propertyValue = "jakarta.jms.Topic"),
        @ActivationConfigProperty(propertyName = "subscriptionDurability",
                propertyValue = "Durable"),
        @ActivationConfigProperty(propertyName = "clientId",
                propertyValue = "TechMartNotificationClient"),
        @ActivationConfigProperty(propertyName = "subscriptionName",
                propertyValue = "NotificationSub"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode",
                propertyValue = "Auto-acknowledge")
})
@Log
public class NotificationMDB extends BaseMessageProcessor<NotificationDTO> {

    @Override
    protected NotificationDTO extractPayload(Message message) throws JMSException {
        if (message instanceof ObjectMessage objMsg) {
            return (NotificationDTO) objMsg.getObject();
        }
        throw new JMSException("Invalid message type for NotificationTopic.");
    }

    @Override
    protected void processMessage(NotificationDTO notification) throws Exception {
        log.info("Dispatching " + notification.type() + " notification to " + notification.customerEmail());

        // Simulate Email/SMS gateway latency
        Thread.sleep(200);

        log.info("Notification sent successfully for Order " + notification.orderNumber());
    }
}
