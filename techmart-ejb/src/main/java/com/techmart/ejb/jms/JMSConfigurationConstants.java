package com.techmart.ejb.jms;

public final class JMSConfigurationConstants {

    private JMSConfigurationConstants() {
    }

    public static final String ORDER_QUEUE = "jms/OrderQueue";

    public static final String INVENTORY_QUEUE = "jms/InventoryQueue";

    public static final String NOTIFICATION_TOPIC = "jms/NotificationTopic";

    public static final String PRICE_CHANGE_TOPIC = "jms/PriceChangeTopic";

    public static final String ANALYTICS_TOPIC = "jms/AnalyticsTopic";


    public static final String ACK_AUTO = "Auto-acknowledge";
}
