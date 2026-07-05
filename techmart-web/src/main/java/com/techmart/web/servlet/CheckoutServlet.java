package com.techmart.web.servlet;

import com.techmart.common.dto.CartItemDTO;
import com.techmart.common.dto.OrderDTO;
import com.techmart.common.entity.Customer;
import com.techmart.common.exception.InsufficientStockException;
import com.techmart.ejb.session.stateless.CartManagerBean;
import com.techmart.ejb.session.stateless.CustomerServiceBean;
import com.techmart.ejb.session.stateless.OrderProcessingBean;
import jakarta.ejb.EJB;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.java.Log;

import java.io.IOException;
import java.util.List;

@WebServlet("/api/checkout")
@Log
public class CheckoutServlet extends HttpServlet {

    @EJB
    private CartManagerBean cartManagerBean;

    @EJB
    private OrderProcessingBean orderProcessingBean;

    @EJB
    private CustomerServiceBean customerServiceBean;

    private static final Long SAMPLE_CUSTOMER_ID = 1L;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Customer sampleCustomer = customerServiceBean.getCustomerProfile(SAMPLE_CUSTOMER_ID);

            if(sampleCustomer == null){
                sampleCustomer = new Customer(
                        "John","Doe","john@gmail.com",
                        "hashPw","+94771234567","Colombo");
                sampleCustomer.setId(SAMPLE_CUSTOMER_ID);
            }

            List<CartItemDTO> cartItems = cartManagerBean.getCartItems(SAMPLE_CUSTOMER_ID);

            if (cartItems.isEmpty()) {
                resp.sendError(400, "Cart is empty");
                return;
            }

            log.info("Checkout - cart items: " + cartItems.size());

            OrderDTO order = orderProcessingBean.processCheckout(sampleCustomer, cartItems, sampleCustomer.getAddress());

            cartManagerBean.removeCart(SAMPLE_CUSTOMER_ID);

            log.info("Checkout successful for Order: " + order.orderNumber());

            resp.setContentType("application/json");
            resp.getWriter().write("{\"status\": \"SUCCESS\", \"orderId\": " + order.id() + ", \"orderNumber\": \"" + order.orderNumber() + "\"}");

        } catch (InsufficientStockException e) {
            log.warning("Checkout failed due to insufficient stock: " + e.getMessage());
            resp.setStatus(400);
            resp.setContentType("application/json");
            resp.getWriter().write("{\"status\": \"FAILED\", \"message\": \"One or more items are out of stock. Please update your cart.\"}");

        } catch (Exception e) {
            log.severe("Checkout failed: " + e.getMessage());
            resp.sendError(500, "Checkout failed: " + e.getMessage());
        }
    }
}
