package com.techmart.ejb.mdb;

import com.techmart.common.dto.OrderDTO;
import com.techmart.ejb.mdb.base.BaseMessageProcessor;
import com.techmart.ejb.session.stateless.OrderProcessingBean;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.MessageDriven;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.ObjectMessage;
import lombok.extern.java.Log;

@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup",
                propertyValue = "jms/OrderQueue"),
        @ActivationConfigProperty(propertyName = "destinationType",
                propertyValue = "jakarta.jms.Queue"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode",
                propertyValue = "Auto-acknowledge")
})
@Log
public class OrderProcessorMDB extends BaseMessageProcessor<OrderDTO> {

    @EJB
    private OrderProcessingBean orderProcessingBean;

    @PostConstruct
    public void init() {
        log.info("OrderProcessorMDB initialized.");
    }

    @PreDestroy
    public void destroy() {
        log.info("OrderProcessorMDB shutting down.");
    }

    @Override
    protected OrderDTO extractPayload(Message message) throws JMSException {
        if (message instanceof ObjectMessage objMsg) {
            return (OrderDTO) objMsg.getObject();
        }
        throw new JMSException("Invalid message type for OrderQueue. Expected ObjectMessage.");
    }

    @Override
    protected void processMessage(OrderDTO order) throws Exception {
        log.info("Processing Order: " + order.orderNumber() + " for customer: " + order.customerEmail());

        orderProcessingBean.confirmOrderAndNotify(order.id());

        log.info("Order " + order.orderNumber() + " successfully processed and moved to CONFIRMED status.");
    }
}
