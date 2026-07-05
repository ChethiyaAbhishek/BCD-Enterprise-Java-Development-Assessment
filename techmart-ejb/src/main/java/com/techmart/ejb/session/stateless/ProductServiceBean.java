package com.techmart.ejb.session.stateless;

import com.techmart.common.dto.ProductDTO;
import com.techmart.common.entity.Product;
import com.techmart.ejb.dao.InventoryDAO;
import com.techmart.ejb.dao.ProductDAO;
import com.techmart.ejb.interceptor.PerformanceMonitored;
import com.techmart.ejb.session.singleton.ProductCacheBean;
import jakarta.ejb.*;
import jakarta.inject.Inject;
import lombok.extern.java.Log;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.stream.Collectors;

@Stateless
@PerformanceMonitored
@TransactionManagement(TransactionManagementType.CONTAINER)
@Log
public class ProductServiceBean {

    @Inject
    private ProductDAO productDAO;

    @Inject
    private InventoryDAO inventoryDAO;

    @Inject
    private ProductCacheBean productCache;

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<ProductDTO> getProducts(int page, int pageSize, String category) {

        List<ProductDTO> cached = productCache.getProducts(page, pageSize, category);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }
        log.warning("Cache returned empty - falling back to database");

        List<Product> products;

        if (category != null && !category.isBlank()) {
            products = productDAO.findByCategory(category, page * pageSize, pageSize);
        } else {
            products = productDAO.findActiveProducts(page * pageSize, pageSize);
        }
        return products.stream().map(this::mapToProductDTO).collect(Collectors.toList());
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public ProductDTO getProductById(Long productId) {
        Product product = productDAO.findById(productId);
        if (product == null) throw new IllegalArgumentException("Product not found: " + productId);
        return mapToProductDTO(product);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<ProductDTO> searchProducts(String searchTerm, int page, int pageSize) {
        if (searchTerm == null || searchTerm.isBlank()) {
            throw new IllegalArgumentException("Search term cannot be blank");
        }
        List<Product> products = productDAO.searchByName(searchTerm, page * pageSize, pageSize);
        return products.stream().map(this::mapToProductDTO).collect(Collectors.toList());
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ProductDTO updateProduct(Long productId, ProductDTO updatedProduct) {
        Product product = productDAO.findById(productId);
        if (product == null) throw new IllegalArgumentException("Product not found: " + productId);

        product.setName(updatedProduct.name());
        product.setDescription(updatedProduct.description());
        product.setSellingPrice(updatedProduct.sellingPrice());
        product.setImageUrl(updatedProduct.imageUrl());

        productDAO.update(product);
        log.info("Product updated: " + productId);
        return mapToProductDTO(product);
    }


    private ProductDTO mapToProductDTO(@NonNull Product product) {
        Long totalStock = inventoryDAO.calculateTotalStock(product.getId());
        String categoryName = product.getCategory() != null ? product.getCategory().getName() : "Uncategorized";

        return new ProductDTO(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getSellingPrice(),
                categoryName,
                product.getImageUrl(),
                totalStock,
                product.getIsActive()
        );
    }
}
