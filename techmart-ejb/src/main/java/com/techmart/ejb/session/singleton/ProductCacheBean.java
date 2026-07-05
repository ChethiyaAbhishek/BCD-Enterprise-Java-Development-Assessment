package com.techmart.ejb.session.singleton;

import com.techmart.common.dto.ProductDTO;
import com.techmart.common.entity.Product;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.java.Log;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
@Lock(LockType.READ)
@Log
public class ProductCacheBean {

    @PersistenceContext(unitName = "techmartPU")
    private EntityManager entityManager;

    private final Map<String, List<ProductDTO>> productCache = new ConcurrentHashMap<>();
    private final AtomicLong lastRefresh = new AtomicLong(0);
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes
    private volatile boolean isRefreshing = false;

    @PostConstruct
    public void init() {
        log.info("ProductCacheBean initialized - warming up cache...");
        refreshCache();
    }

    public List<ProductDTO> getProducts(int page, int pageSize, String category) {
        String categoryKey = buildKey(0, 1000, category);
        List<ProductDTO> allProducts = productCache.get(categoryKey);

        if (allProducts == null || allProducts.isEmpty() ||
                (System.currentTimeMillis() - lastRefresh.get() > CACHE_TTL_MS && !isRefreshing)) {

            if (!isRefreshing) {
                isRefreshing = true;
                try { refreshCache(); } finally { isRefreshing = false; }
            }
            allProducts = productCache.get(categoryKey);
        }

        if (allProducts == null) return List.of();

        int start = page * pageSize;
        int end = Math.min(start + pageSize, allProducts.size());
        if (start >= allProducts.size()) return List.of();

        log.info("CACHE HIT - Serving " + (end - start) + " products");
        return allProducts.subList(start, end);
    }

    @Lock(LockType.WRITE)
    public void refreshCache() {
        long startTime = System.currentTimeMillis();
        log.info("Starting cache refresh...");

        try {
            List<Product> products = entityManager.createQuery(
                    "SELECT p FROM Product p WHERE p.isActive = true", Product.class
            ).getResultList();

            List<ProductDTO> allProducts = products.stream().map(this::toDTO).collect(Collectors.toList());

            productCache.clear();
            productCache.put(buildKey(0, 1000, null), allProducts);
            productCache.put(buildKey(0, 100, null), allProducts);

            Map<String, List<ProductDTO>> byCategory = allProducts.stream()
                    .filter(p -> p.categoryName() != null)
                    .collect(Collectors.groupingBy(ProductDTO::categoryName));

            for (Map.Entry<String, List<ProductDTO>> entry : byCategory.entrySet()) {
                productCache.put(buildKey(0, 1000, entry.getKey()), entry.getValue());
                productCache.put(buildKey(0, 100, entry.getKey()), entry.getValue());
            }

            lastRefresh.set(System.currentTimeMillis());
            log.info("Cache refresh complete. Products: " + allProducts.size() + ", Duration: " + (System.currentTimeMillis() - startTime) + "ms");
        } catch (Exception e) {
            log.severe("Cache refresh failed: " + e.getMessage());
        }
    }

    private String buildKey(int page, int pageSize, String category) {
        return page + "_" + pageSize + "_" + (category != null ? category.toLowerCase() : "all");
    }

    private ProductDTO toDTO(Product p) {
        return new ProductDTO(
                p.getId(), p.getSku(), p.getName(), p.getDescription(), p.getSellingPrice(),
                p.getCategory() != null ? p.getCategory().getName() : null,
                p.getImageUrl(), 100L, p.getIsActive()
        );
    }
}