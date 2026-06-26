package com.techmart.web.servlet;

import com.techmart.common.dto.CartItemDTO;
import com.techmart.ejb.session.stateless.CartManagerBean;
import jakarta.ejb.EJB;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.java.Log;

import java.io.IOException;
import java.math.BigDecimal;

@WebServlet("/api/cart")
@Log
public class ShoppingCartServlet extends HttpServlet {

    @EJB
    private CartManagerBean cartManagerBean;

    private static final Long SAMPLE_CUSTOMER_ID = 1L;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Long productId = Long.parseLong(req.getParameter("productId"));
            int qty = Integer.parseInt(req.getParameter("quantity"));
            BigDecimal price = new BigDecimal(req.getParameter("price"));
            String name = req.getParameter("name");

            CartItemDTO item = new CartItemDTO(
                    productId,
                    name != null ? name : "Product",
                    "SKU-" + productId,
                    price,
                    qty,
                    "/img.jpg",
                    "Category"
            );

            cartManagerBean.addItem(SAMPLE_CUSTOMER_ID, item);

            int totalItems = cartManagerBean.getCartItems(SAMPLE_CUSTOMER_ID).size();
            BigDecimal subtotal = cartManagerBean.getCartSubtotal(SAMPLE_CUSTOMER_ID);

            resp.setContentType("application/json");
            resp.getWriter().write(String.format(
                    "{\"status\": \"Added\", \"totalItems\": %d, \"subtotal\": %.2f}",
                    totalItems,
                    subtotal
            ));

        } catch (Exception e) {
            log.severe("Add to cart failed: " + e.getMessage());
            resp.sendError(500, e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            int items = cartManagerBean.getCartItems(SAMPLE_CUSTOMER_ID).size();
            BigDecimal subtotal = cartManagerBean.getCartSubtotal(SAMPLE_CUSTOMER_ID);

            resp.setContentType("application/json");
            resp.getWriter().write(String.format(
                    "{\"items\": %d, \"subtotal\": %.2f}",
                    items,
                    subtotal
            ));

        } catch (Exception e) {
            log.severe("Get cart failed: " + e.getMessage());
            resp.sendError(500, e.getMessage());
        }
    }
}