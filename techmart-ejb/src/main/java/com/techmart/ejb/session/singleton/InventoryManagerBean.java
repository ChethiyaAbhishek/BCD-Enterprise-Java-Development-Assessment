package com.techmart.ejb.session.singleton;

import com.techmart.common.dto.AllocationResult;
import com.techmart.common.dto.InventoryUpdateDTO;
import com.techmart.common.entity.*;
import com.techmart.common.exception.InsufficientStockException;
import com.techmart.ejb.interceptor.PerformanceMonitored;
import com.techmart.ejb.jms.JMSConfigurationConstants;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.*;
import jakarta.inject.Inject;
import jakarta.jms.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.extern.java.Log;

import java.lang.IllegalStateException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Singleton
@Startup
@Lock(LockType.READ)
@PerformanceMonitored
@Log
public class InventoryManagerBean {

    @PersistenceContext(unitName = "techmartPU")
    private EntityManager entityManager;

    @Inject
    private JMSContext jmsContext;

    @Resource(lookup = JMSConfigurationConstants.NOTIFICATION_TOPIC)
    private Topic notificationTopic;

    @Resource(lookup = JMSConfigurationConstants.INVENTORY_QUEUE)
    private Queue inventoryQueue;

    /**
     * Using ConcurrentHashMap to ensure thread-safety for global cache
     * (productId, warehouseId) -> Available Quantity
     */
    private final Map<String, Integer> stockCache = new ConcurrentHashMap<>();

    /**
     * Cache for batch availability to speed up allocation
     */
    private final Map<Long, List<InventoryBatch>> batchCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init(){
        loadInventoryCache();
        loadBatchCache();
    }

    @PerformanceMonitored(value = "Inventory.getStock", slowThresholdMs = 10)
    public int getAvailableStock(Long productId, Long warehouseId){
        String key = buildCacheKey(productId, warehouseId);
        return stockCache.getOrDefault(key, 0);
    }

    public int getTotalAvailableStock(Long productId){
        return stockCache.entrySet().stream()
                .filter(entry-> entry.getKey().startsWith(productId+":"))
                .mapToInt(Map.Entry::getValue).sum();
    }

    public boolean isProductAvailable(Long productId, int requiredQty){
        return getTotalAvailableStock(productId) >= requiredQty;
    }

    @Lock(LockType.WRITE)
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void addNewBatch(Long productId, Long warehouseId, String batchNumber, BigDecimal costPrice, int qty, String supplier){
        Inventory inventory = findOrCreateInventory(productId, warehouseId);

        InventoryBatch batch = InventoryBatch.builder()
                .inventory(inventory)
                .batchNumber(batchNumber)
                .supplierName(supplier)
                .costPrice(costPrice)
                .receivedQuantity(qty)
                .availableQuantity(qty)
                .receivedDate(LocalDate.now()).build();

        inventory.addBatch(batch);
        entityManager.persist(batch);
        entityManager.merge(inventory);

        updateCache(productId, warehouseId, inventory.getAvailableQuantity());

        publishInventoryUpdate(productId, warehouseId, qty, "BATCH_RECEIVED", batchNumber);
    }

    @Lock(LockType.WRITE)
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<AllocationResult> allocateOrderItems(List<OrderItem> orderItems) {
        List<AllocationResult> allAllocations = new ArrayList<>();

        for (OrderItem item : orderItems) {
            int remaining = item.getQuantity();
            List<AllocationResult> itemAllocations = new ArrayList<>();

            List<InventoryBatch> availableBatches = getAvailableBatchesForProduct(item.getProduct().getId());

            if (availableBatches.isEmpty()) {
                throw new InsufficientStockException(item.getProduct().getId(), item.getQuantity(), 0);
            }

            for (InventoryBatch batch : availableBatches) {
                if (remaining <= 0) break;

                int available = batch.getAvailableQuantity();
                if (available <= 0) continue;

                int allocate = Math.min(available, remaining);

                batch.reserve(allocate);
                entityManager.merge(batch);

                Inventory inv = batch.getInventory();
                inv.reserve(allocate);
                entityManager.merge(inv);

                itemAllocations.add(AllocationResult.builder()
                        .orderItem(item)
                        .batch(batch)
                        .quantity(allocate)
                        .costPrice(batch.getCostPrice())
                        .build());

                remaining -= allocate;

                updateCache(item.getProduct().getId(),
                        batch.getInventory().getWarehouse().getId(),
                        inv.getAvailableQuantity());
            }

            if (remaining > 0) {
                throw new InsufficientStockException(
                        item.getProduct().getId(), item.getQuantity(), item.getQuantity() - remaining
                );
            }

            allAllocations.addAll(itemAllocations);
        }

        return allAllocations;
    }

    @Lock(LockType.WRITE)
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void releaseReservation(Long productId, Long warehouseId, int quantity) {
        Inventory inventory = findInventory(productId, warehouseId);
        if (inventory == null) {
            throw new IllegalStateException("Inventory not found for product " + productId + " at warehouse " + warehouseId);
        }

        inventory.releaseReservation(quantity);
        entityManager.merge(inventory);

        List<InventoryBatch> reservedBatches = entityManager.createQuery(
                        "SELECT b FROM InventoryBatch b " +
                                "WHERE b.inventory.id = :invId AND b.reservedQuantity > 0 " +
                                "ORDER BY b.costPrice DESC, b.receivedDate DESC",
                        InventoryBatch.class)
                .setParameter("invId", inventory.getId())
                .getResultList();

        int remainingToRelease = quantity;
        for (InventoryBatch batch : reservedBatches) {
            if (remainingToRelease <= 0) break;

            int releaseFromBatch = Math.min(batch.getReservedQuantity(), remainingToRelease);
            batch.releaseReservation(releaseFromBatch);
            entityManager.merge(batch);
            remainingToRelease -= releaseFromBatch;
        }

        if (remainingToRelease > 0) {
            throw new IllegalStateException(
                    "Could not release full quantity. Remaining: " + remainingToRelease
            );
        }

        updateCache(productId, warehouseId, inventory.getAvailableQuantity());
    }



    private void loadInventoryCache() {
        List<Inventory> inventories = entityManager.createQuery(
                "SELECT i FROM Inventory i", Inventory.class).getResultList();

        for (Inventory inv : inventories) {
            String key = buildCacheKey(inv.getProduct().getId(), inv.getWarehouse().getId());
            stockCache.put(key, inv.getAvailableQuantity());
        }
    }

    private void loadBatchCache() {
        List<InventoryBatch> batches = entityManager.createQuery(
                "SELECT b FROM InventoryBatch b WHERE b.availableQuantity > 0 " +
                        "ORDER BY b.costPrice ASC", InventoryBatch.class).getResultList();
        Map<Long, List<InventoryBatch>> grouped = batches.stream()
                .collect(Collectors.groupingBy(b -> b.getInventory().getProduct().getId()));

        batchCache.putAll(grouped);
    }

    private List<InventoryBatch> getAvailableBatchesForProduct(Long productId) {
        List<InventoryBatch> batches = batchCache.get(productId);
        if (batches != null && !batches.isEmpty()) {
            return batches.stream()
                    .filter(b -> b.getAvailableQuantity() > 0)
                    .collect(Collectors.toList());
        }

        TypedQuery<InventoryBatch> query = entityManager.createQuery(
                "SELECT b FROM InventoryBatch b " +
                        "WHERE b.inventory.product.id = :pid AND b.availableQuantity > 0 " +
                        "ORDER BY b.costPrice ASC, b.receivedDate ASC",
                InventoryBatch.class);
        query.setParameter("pid", productId);
        return query.getResultList();
    }

    private Inventory findOrCreateInventory(Long productId, Long warehouseId) {
        Inventory inv = findInventory(productId, warehouseId);
        if (inv == null) {
            inv = new Inventory();
            inv.setProduct(entityManager.find(Product.class, productId));
            inv.setWarehouse(entityManager.find(Warehouse.class, warehouseId));
            entityManager.persist(inv);
        }
        return inv;
    }

    private Inventory findInventory(Long productId, Long warehouseId) {
        try {
            return entityManager.createQuery(
                            "SELECT i FROM Inventory i WHERE i.product.id = :pid AND i.warehouse.id = :wid",
                            Inventory.class)
                    .setParameter("pid", productId)
                    .setParameter("wid", warehouseId)
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    private void updateCache(Long productId, Long warehouseId, int newQuantity) {
        String key = buildCacheKey(productId, warehouseId);
        stockCache.put(key, newQuantity);
    }

    private String buildCacheKey(Long productId, Long warehouseId) {
        return productId + ":" + warehouseId;
    }

    private void publishInventoryUpdate(Long productId, Long warehouseId,
                                        int changeAmount, String reason, String batchNumber) {
        try {
            InventoryUpdateDTO dto = new InventoryUpdateDTO(
                    productId, warehouseId, changeAmount, reason, batchNumber
            );

            jmsContext.createProducer().send(inventoryQueue, dto);

            jmsContext.createProducer().send(notificationTopic, dto);

        } catch (JMSRuntimeException e) {
            log.severe("Failed to publish inventory update: " + e.getMessage());
        }
    }

}
