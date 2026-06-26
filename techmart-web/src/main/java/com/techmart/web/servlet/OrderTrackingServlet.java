package com.techmart.web.servlet;

import com.techmart.common.dto.TrackingResult;
import com.techmart.ejb.session.stateless.OrderTrackingService;
import jakarta.ejb.EJB;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.java.Log;

import java.io.IOException;

@WebServlet("/api/order-tracking")
@Log
public class OrderTrackingServlet extends HttpServlet {

    @EJB
    private OrderTrackingService orderTrackingService;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String orderNumber = req.getParameter("orderNumber");

        if (orderNumber == null || orderNumber.isBlank()) {
            resp.sendError(400, "Missing orderNumber parameter");
            return;
        }

        try {
            TrackingResult result = orderTrackingService.getTrackingStatus(orderNumber);

            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");

            String json = String.format(
                    "{" +
                            "\"orderNumber\": \"%s\", " +
                            "\"status\": \"%s\", " +
                            "\"location\": \"%s\", " +
                            "\"estimatedDelivery\": \"%s\"" +
                            "}",
                    orderNumber,
                    result.status(),
                    result.location(),
                    result.estimatedDelivery()
            );

            log.info("Tracking info retrieved for order: " + orderNumber);
            resp.getWriter().write(json);

        } catch (Exception e) {
            log.severe("Failed to get tracking info: " + e.getMessage());
            resp.sendError(500, "Failed to retrieve tracking: " + e.getMessage());
        }
    }
}
