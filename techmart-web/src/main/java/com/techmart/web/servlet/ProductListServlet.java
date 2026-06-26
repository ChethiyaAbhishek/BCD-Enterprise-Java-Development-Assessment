package com.techmart.web.servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.techmart.common.dto.ProductDTO;
import com.techmart.ejb.session.stateless.ProductServiceBean;
import jakarta.ejb.EJB;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.java.Log;

import java.io.IOException;
import java.util.List;

@WebServlet("/api/products")
@Log
public class ProductListServlet extends HttpServlet {

    @EJB
    private ProductServiceBean productService;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String category = req.getParameter("category");
        int page = req.getParameter("page") != null ? Integer.parseInt(req.getParameter("page")) : 0;

        List<ProductDTO> products = productService.getProducts(page, 20, category);

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(GSON.toJson(products));
    }
}
