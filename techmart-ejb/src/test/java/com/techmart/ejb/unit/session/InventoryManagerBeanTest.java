package com.techmart.ejb.unit.session;

import com.techmart.common.dto.AllocationResult;
import com.techmart.common.entity.Inventory;
import com.techmart.common.entity.InventoryBatch;
import com.techmart.common.entity.OrderItem;
import com.techmart.common.entity.Product;
import com.techmart.common.entity.Warehouse;
import com.techmart.ejb.dao.InventoryDAO;
import com.techmart.ejb.session.singleton.InventoryManagerBean;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InventoryManagerBean (Singleton)
 * Validates inventory allocation and concurrency handling.
 */
@ExtendWith(MockitoExtension.class)
class InventoryManagerBeanTest {

    @InjectMocks
    private InventoryManagerBean inventoryManager;

    @Mock
    private EntityManager entityManager;

    @Mock
    private InventoryDAO inventoryDAO;

    @Test
    void testAllocateOrderItemsSuccessfully() {
        OrderItem orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setQuantity(2);

        Product product = new Product();
        product.setId(1L);
        orderItem.setProduct(product);

        Warehouse warehouse = new Warehouse();
        warehouse.setId(1L);
        warehouse.setCode("WH-001");

        Inventory inventory = new Inventory();
        inventory.setId(1L);
        inventory.setProduct(product);
        inventory.setWarehouse(warehouse);
        inventory.setQuantity(10);

        InventoryBatch batch = new InventoryBatch();
        batch.setId(1L);
        batch.setBatchNumber("BATCH-001");
        batch.setInventory(inventory);
        batch.setAvailableQuantity(10);
        batch.setCostPrice(BigDecimal.valueOf(100000));

        try {
            java.lang.reflect.Field batchCacheField = InventoryManagerBean.class.getDeclaredField("batchCache");
            batchCacheField.setAccessible(true);
            java.util.Map<Long, List<InventoryBatch>> batchCache =
                    (java.util.Map<Long, List<InventoryBatch>>) batchCacheField.get(inventoryManager);

            batchCache.put(1L, List.of(batch));

            when(entityManager.merge(any(InventoryBatch.class))).thenReturn(batch);
            when(entityManager.merge(any(Inventory.class))).thenReturn(inventory);

            List<AllocationResult> results = inventoryManager.allocateOrderItems(List.of(orderItem));

            assertNotNull(results);
            assertEquals(1, results.size());
            assertEquals(2, results.get(0).getQuantity());
            assertEquals(batch, results.get(0).getBatch());

        } catch (Exception e) {
            fail("Failed to access batchCache: " + e.getMessage());
        }
    }

    @Test
    void testGetAvailableStock() {
        int stock = inventoryManager.getAvailableStock(999L, 999L);
        assertEquals(0, stock);

        try {
            java.lang.reflect.Field stockCacheField = InventoryManagerBean.class.getDeclaredField("stockCache");
            stockCacheField.setAccessible(true);
            java.util.Map<String, Integer> stockCache =
                    (java.util.Map<String, Integer>) stockCacheField.get(inventoryManager);

            stockCache.put("1:1", 50);

            int actualStock = inventoryManager.getAvailableStock(1L, 1L);

            assertEquals(50, actualStock);
        } catch (Exception e) {
            fail("Failed to access stockCache: " + e.getMessage());
        }
    }
}
