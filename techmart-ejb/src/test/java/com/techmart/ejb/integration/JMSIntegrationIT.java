package com.techmart.ejb.integration;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.jms.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for JMS messaging infrastructure
 */
@ExtendWith(ArquillianExtension.class)
class JMSIntegrationIT {

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "jms-test.war")
                .addPackages(true, "com.techmart.common")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource("META-INF/persistence.xml", "META-INF/persistence.xml");
    }

    @Inject
    private JMSContext jmsContext;

    @Resource(lookup = "jms/OrderQueue")
    private Queue orderQueue;

    @Resource(lookup = "jms/NotificationTopic")
    private Topic notificationTopic;

    /**
     * Test 1: Verify JMSContext can be injected
     */
    @Test
    void testJMSContextInjection() {
        assertNotNull(jmsContext, "JMSContext should be injected by the container");
    }

    /**
     * Test 2: Verify Queue resource can be injected
     */
    @Test
    void testQueueResourceInjection() {
        assertNotNull(orderQueue, "OrderQueue should be injected via @Resource");
        assertTrue(orderQueue instanceof Queue, "Should be a JMS Queue");
    }

    /**
     * Test 3: Verify Topic resource can be injected
     */
    @Test
    void testTopicResourceInjection() {
        assertNotNull(notificationTopic, "NotificationTopic should be injected via @Resource");
        assertTrue(notificationTopic instanceof Topic, "Should be a JMS Topic");
    }

    /**
     * Test 4: Verify JMSProducer can be created
     */
    @Test
    void testCreateJMSProducer() {
        JMSProducer producer = jmsContext.createProducer();
        assertNotNull(producer, "JMSProducer should be created successfully");
    }

    /**
     * Test 5: Verify message can be sent to queue (proves JMS connection works)
     */
    @Test
    void testSendMessageToQueue() {
        String testMessage = "TEST_MESSAGE_" + System.currentTimeMillis();

        assertDoesNotThrow(() -> {
            jmsContext.createProducer().send(orderQueue, testMessage);
        }, "Should be able to send message to queue without exception");
    }

    /**
     * Test 6: Verify message can be sent to topic (proves JMS connection works)
     */
    @Test
    void testSendMessageToTopic() {
        String testMessage = "TEST_NOTIFICATION_" + System.currentTimeMillis();

        assertDoesNotThrow(() -> {
            jmsContext.createProducer().send(notificationTopic, testMessage);
        }, "Should be able to send message to topic without exception");
    }

    /**
     * Test 7: Verify JMSContext can create consumer (proves JMS API works)
     */
    @Test
    void testCreateJMSConsumer() {
        assertDoesNotThrow(() -> {
            JMSConsumer consumer = jmsContext.createConsumer(orderQueue);
            assertNotNull(consumer, "JMSConsumer should be created");
            consumer.close();
        }, "Should be able to create JMS consumer");
    }
}