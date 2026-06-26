package com.techmart.web.servlet;

import com.techmart.common.dto.OrderDTO;
import com.techmart.ejb.session.stateless.OrderProcessingBean;
import jakarta.ejb.EJB;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.java.Log;

import java.io.IOException;
import java.util.List;

@WebServlet("/api/orders")
@Log
public class OrderHistoryServlet extends HttpServlet {

    @EJB
    private OrderProcessingBean orderProcessingBean;

    private static final Long SAMPLE_CUSTOMER_ID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            int page = req.getParameter("page") != null ? Integer.parseInt(req.getParameter("page")) : 0;
            int pageSize = 10;

            List<OrderDTO> orders = orderProcessingBean.getCustomerOrders(SAMPLE_CUSTOMER_ID, page, pageSize);

            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");

            StringBuilder json = new StringBuilder();
            json.append("{\"orders\": [");

            for (int i = 0; i < orders.size(); i++) {
                OrderDTO order = orders.get(i);
                json.append("{");
                json.append("\"id\": ").append(order.id()).append(",");
                json.append("\"orderNumber\": \"").append(order.orderNumber()).append("\",");
                json.append("\"status\": \"").append(order.status()).append("\",");
                json.append("\"paymentStatus\": \"").append(order.paymentStatus()).append("\",");
                json.append("\"totalAmount\": ").append(order.totalAmount()).append(",");
                json.append("\"itemCount\": ").append(order.items() != null ? order.items().size() : 0).append(",");
                json.append("\"createdAt\": \"").append(order.createdAt()).append("\"");
                json.append("}");

                if (i < orders.size() - 1) {
                    json.append(",");
                }
            }

            json.append("], \"page\": ").append(page).append(", \"hasMore\": ").append(orders.size() == pageSize).append("}");

            resp.getWriter().write(json.toString());

        } catch (Exception e) {
            log.severe("Failed to get order history: " + e.getMessage());
            resp.sendError(500, "Failed to retrieve orders: " + e.getMessage());
        }
    }
}
