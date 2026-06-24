package com.techmart.ejb.jms;

public final class JMSConfigurationConstants {

    private JMSConfigurationConstants() {
    }

    /**
     * Used by OrderProcessorMDB and InventorySyncMDB
     */
    public static final String QUEUE_CONNECTION_FACTORY = "jms/OrderQueueFactory";

    /**
     * Used by NotificationMDB and AnalyticsEventMDB
     */
    public static final String TOPIC_CONNECTION_FACTORY = "jms/NotificationTopicFactory";


    public static final String ORDER_QUEUE = "jms/OrderQueue";

    public static final String INVENTORY_QUEUE = "jms/InventoryQueue";

    public static final String NOTIFICATION_TOPIC = "jms/NotificationTopic";

    public static final String PRICE_CHANGE_TOPIC = "jms/PriceChangeTopic";

    public static final String ANALYTICS_TOPIC = "jms/AnalyticsTopic";


    public static final String ACK_AUTO = "Auto-acknowledge";

    public static final String ACK_CLIENT = "Client-acknowledge";
}
