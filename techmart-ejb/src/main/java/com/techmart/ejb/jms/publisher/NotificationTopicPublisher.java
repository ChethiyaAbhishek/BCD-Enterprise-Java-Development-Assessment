package com.techmart.ejb.jms.publisher;

import com.techmart.common.dto.NotificationDTO;
import com.techmart.ejb.jms.JMSConfigurationConstants;
import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.jms.DeliveryMode;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSRuntimeException;
import jakarta.jms.Topic;
import lombok.extern.java.Log;

@Stateless
@Log
public class NotificationTopicPublisher {

    @Inject
    private JMSContext jmsContext;

    @Resource(lookup = JMSConfigurationConstants.NOTIFICATION_TOPIC)
    private Topic notificationTopic;

    public void publishNotification(NotificationDTO notification) {
        try {
            jmsContext.createProducer()
                    .setDeliveryMode(DeliveryMode.PERSISTENT)
                    .setPriority(8)
                    .send(notificationTopic, notification);

            log.info("Notification published for customer " + notification.customerId() + " (Type: " + notification.type() + ")");
        } catch (JMSRuntimeException e) {
            log.severe("Failed to publish notification: " + e.getMessage());
        }
    }
}
