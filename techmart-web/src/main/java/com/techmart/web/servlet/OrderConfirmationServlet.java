package com.techmart.web.servlet;

import com.techmart.common.entity.Order;
import com.techmart.ejb.dao.OrderDAO;
import jakarta.ejb.EJB;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.java.Log;

import java.io.IOException;

@WebServlet("/api/order-status")
@Log
public class OrderConfirmationServlet extends HttpServlet {

    @EJB
    private OrderDAO orderDAO;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String orderIdParam = req.getParameter("orderId");

        if (orderIdParam == null || orderIdParam.isBlank()) {
            resp.sendError(400, "Missing orderId parameter");
            return;
        }

        try {
            Long orderId = Long.parseLong(orderIdParam);
            Order order = orderDAO.findById(orderId);

            resp.setContentType("application/json");
            if (order != null) {
                resp.getWriter().write("{\"orderId\": " + order.getId() + ", \"status\": \"" + order.getStatus() + "\", \"paymentStatus\": \"" + order.getPaymentStatus() + "\"}");
            } else {
                resp.getWriter().write("{\"error\": \"Order not found\"}");
            }
        } catch (NumberFormatException e) {
            resp.sendError(400, "Invalid orderId format");
        }
    }
}
