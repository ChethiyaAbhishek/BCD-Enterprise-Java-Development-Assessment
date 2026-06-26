package com.techmart.web.servlet;

import com.techmart.common.enums.OrderStatus;
import com.techmart.ejb.dao.OrderDAO;
import com.techmart.ejb.dao.ProductDAO;
import jakarta.ejb.EJB;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.java.Log;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@WebServlet("/api/admin/dashboard")
@Log
public class AdminDashboardServlet extends HttpServlet {

    @EJB
    private OrderDAO orderDAO;

    @EJB
    private ProductDAO productDAO;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            LocalDateTime startDate = LocalDateTime.now().minusDays(30);
            LocalDateTime endDate = LocalDateTime.now();

            long pendingOrders = orderDAO.countByStatus(OrderStatus.PENDING);
            long confirmedOrders = orderDAO.countByStatus(OrderStatus.CONFIRMED);
            long shippedOrders = orderDAO.countByStatus(OrderStatus.SHIPPED);
            long deliveredOrders = orderDAO.countByStatus(OrderStatus.DELIVERED);
            long cancelledOrders = orderDAO.countByStatus(OrderStatus.CANCELLED);

            BigDecimal revenue = orderDAO.calculateRevenue(startDate, endDate);

            long totalProducts = productDAO.count();

            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");

            String json = String.format(
                    "{" +
                            "\"revenue\": %.2f, " +
                            "\"pendingOrders\": %d, " +
                            "\"confirmedOrders\": %d, " +
                            "\"shippedOrders\": %d, " +
                            "\"deliveredOrders\": %d, " +
                            "\"cancelledOrders\": %d, " +
                            "\"totalProducts\": %d" +
                            "}",
                    revenue != null ? revenue : BigDecimal.ZERO,
                    pendingOrders,
                    confirmedOrders,
                    shippedOrders,
                    deliveredOrders,
                    cancelledOrders,
                    totalProducts
            );

            log.info("Admin dashboard data retrieved successfully");
            resp.getWriter().write(json);

        } catch (Exception e) {
            log.severe("Failed to load admin dashboard: " + e.getMessage());
            resp.sendError(500, "Failed to load dashboard data: " + e.getMessage());
        }
    }
}
