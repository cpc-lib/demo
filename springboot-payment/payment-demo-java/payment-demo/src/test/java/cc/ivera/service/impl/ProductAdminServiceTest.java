package cc.ivera.service.impl;

import cc.ivera.dto.ProductCreateRequest;
import cc.ivera.dto.ProductStatusRequest;
import cc.ivera.dto.ProductUpdateRequest;
import cc.ivera.dto.StockAdjustmentRequest;
import cc.ivera.entity.InventoryOperation;
import cc.ivera.entity.Product;
import cc.ivera.entity.ProductSku;
import cc.ivera.entity.SkuStock;
import cc.ivera.enums.ProductStatus;
import cc.ivera.enums.UserRole;
import cc.ivera.exception.ConflictException;
import cc.ivera.exception.NotFoundException;
import cc.ivera.mapper.InventoryOperationMapper;
import cc.ivera.mapper.ProductMapper;
import cc.ivera.mapper.ProductSkuMapper;
import cc.ivera.mapper.SkuStockMapper;
import cc.ivera.security.AuthUser;
import cc.ivera.service.ProductAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductAdminServiceTest {

    private ProductMapper productMapper;
    private ProductSkuMapper productSkuMapper;
    private SkuStockMapper skuStockMapper;
    private InventoryOperationMapper operationMapper;
    private ProductAdminService service;
    private AuthUser admin;

    @BeforeEach
    void setUp() {
        productMapper = mock(ProductMapper.class);
        productSkuMapper = mock(ProductSkuMapper.class);
        skuStockMapper = mock(SkuStockMapper.class);
        operationMapper = mock(InventoryOperationMapper.class);
        service = new ProductAdminServiceImpl(
                productMapper,
                productSkuMapper,
                skuStockMapper,
                operationMapper
        );
        admin = new AuthUser(3L, "operator", UserRole.ADMIN);
        when(productSkuMapper.insert(any(ProductSku.class))).thenAnswer(invocation -> {
            ProductSku sku = invocation.getArgument(0);
            sku.setId(sku.getProductId() * 10);
            return 1;
        });
        when(skuStockMapper.insert(any(SkuStock.class))).thenReturn(1);
        when(operationMapper.insert(any(InventoryOperation.class))).thenReturn(1);
    }

    @Test
    void createDefaultsToOffShelfAndRecordsPositiveInitialStock() {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setTitle("并发课程");
        request.setPrice(19900);
        request.setInitialStock(8);
        when(productMapper.insert(any(Product.class))).thenAnswer(invocation -> {
            ((Product) invocation.getArgument(0)).setId(7L);
            return 1;
        });
        when(productMapper.selectByIdWithDefaultStock(7L))
                .thenReturn(product(7L, ProductStatus.OFF_SHELF, 8, 0, 0, 0));

        Product created = service.create(request, admin);

        assertEquals(ProductStatus.OFF_SHELF, created.getStatus());
        assertEquals(8, created.getAvailableStock());
        assertEquals(0, created.getLockedStock());
        assertEquals(0, created.getSoldStock());
        assertEquals(0, created.getVersion());

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).insert(productCaptor.capture());
        assertEquals(null, productCaptor.getValue().getAvailableStock());
        assertEquals(null, productCaptor.getValue().getLockedStock());
        assertEquals(null, productCaptor.getValue().getSoldStock());
        ArgumentCaptor<ProductSku> skuCaptor = ArgumentCaptor.forClass(ProductSku.class);
        verify(productSkuMapper).insert(skuCaptor.capture());
        assertEquals(7L, skuCaptor.getValue().getProductId());
        assertEquals("DEFAULT-7", skuCaptor.getValue().getSkuCode());
        ArgumentCaptor<SkuStock> stockCaptor = ArgumentCaptor.forClass(SkuStock.class);
        verify(skuStockMapper).insert(stockCaptor.capture());
        assertEquals(70L, stockCaptor.getValue().getSkuId());
        assertEquals(8, stockCaptor.getValue().getAvailableStock());
        assertEquals(0, stockCaptor.getValue().getLockedStock());

        ArgumentCaptor<InventoryOperation> captor = ArgumentCaptor.forClass(InventoryOperation.class);
        verify(operationMapper).insert(captor.capture());
        InventoryOperation operation = captor.getValue();
        assertEquals("ADMIN_ADJUST:__CREATE__:7", operation.getBusinessKey());
        assertEquals("ADMIN_ADJUST", operation.getOperationType());
        assertEquals(70L, operation.getSkuId());
        assertEquals(8, operation.getAvailableDelta());
        assertEquals(0, operation.getAvailableBefore());
        assertEquals(8, operation.getAvailableAfter());
        assertEquals(3L, operation.getOperatorId());
        assertEquals("operator", operation.getOperatorName());
    }

    @Test
    void createWithZeroStockDoesNotWriteAnInventoryChange() {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setTitle("空库存课程");
        request.setPrice(9900);
        request.setInitialStock(0);
        when(productMapper.insert(any(Product.class))).thenAnswer(invocation -> {
            ((Product) invocation.getArgument(0)).setId(8L);
            return 1;
        });
        when(productMapper.selectByIdWithDefaultStock(8L))
                .thenReturn(product(8L, ProductStatus.OFF_SHELF, 0, 0, 0, 0));

        service.create(request, admin);

        verify(productSkuMapper).insert(any(ProductSku.class));
        verify(skuStockMapper).insert(any(SkuStock.class));
        verify(operationMapper, never()).insert(any(InventoryOperation.class));
    }

    @Test
    void createReturnsPersistedRowWithDatabaseTimestamps() {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setTitle("持久化商品");
        request.setPrice(100);
        request.setInitialStock(0);
        when(productMapper.insert(any(Product.class))).thenAnswer(invocation -> {
            ((Product) invocation.getArgument(0)).setId(9L);
            return 1;
        });
        Product persisted = product(9L, ProductStatus.OFF_SHELF, 0, 0, 0, 0);
        persisted.setCreateTime(new Date());
        when(productMapper.selectByIdWithDefaultStock(9L)).thenReturn(persisted);

        assertSame(persisted, service.create(request, admin));
    }

    @Test
    void updateBasicInfoRequiresCurrentVersionAndPreservesInventory() {
        Product locked = product(7L, ProductStatus.ON_SHELF, 10, 2, 3, 4);
        Product persisted = product(7L, ProductStatus.ON_SHELF, 10, 2, 3, 5);
        persisted.setTitle("新名称");
        persisted.setPrice(2500);
        when(productMapper.selectByIdForUpdate(7L)).thenReturn(locked);
        when(productMapper.updateBasicInfo(7L, "新名称", 2500, 4)).thenReturn(1);
        when(productMapper.selectByIdWithDefaultStock(7L)).thenReturn(persisted);
        ProductUpdateRequest request = updateRequest("新名称", 2500, 4);

        Product updated = service.update(7L, request, admin);

        verify(productMapper).updateBasicInfo(7L, "新名称", 2500, 4);
        assertEquals("新名称", updated.getTitle());
        assertEquals(2500, updated.getPrice());
        assertEquals(10, updated.getAvailableStock());
        assertEquals(2, updated.getLockedStock());
        assertEquals(3, updated.getSoldStock());
        assertEquals(5, updated.getVersion());
    }

    @Test
    void updateReturnsRefreshedRowAfterDatabaseUpdate() {
        Product locked = product(7L, ProductStatus.ON_SHELF, 10, 2, 3, 4);
        Product persisted = product(7L, ProductStatus.ON_SHELF, 10, 2, 3, 5);
        persisted.setTitle("新名称");
        persisted.setPrice(2500);
        persisted.setUpdateTime(new Date());
        when(productMapper.selectByIdForUpdate(7L)).thenReturn(locked);
        when(productMapper.updateBasicInfo(7L, "新名称", 2500, 4)).thenReturn(1);
        when(productMapper.selectByIdWithDefaultStock(7L)).thenReturn(persisted);

        assertSame(persisted, service.update(7L, updateRequest("新名称", 2500, 4), admin));
    }

    @Test
    void staleProductVersionIsRejectedBeforeUpdate() {
        when(productMapper.selectByIdForUpdate(7L))
                .thenReturn(product(7L, ProductStatus.ON_SHELF, 10, 0, 0, 4));

        assertThrows(ConflictException.class,
                () -> service.update(7L, updateRequest("新名称", 2500, 3), admin));

        verify(productMapper, never()).updateBasicInfo(any(), any(), any(), any());
    }

    @Test
    void statusChangeOnlyChangesStatusAndVersion() {
        Product locked = product(7L, ProductStatus.OFF_SHELF, 10, 2, 3, 4);
        Product persisted = product(7L, ProductStatus.ON_SHELF, 10, 2, 3, 5);
        when(productMapper.selectByIdForUpdate(7L)).thenReturn(locked);
        when(productMapper.updateStatus(7L, ProductStatus.ON_SHELF, 4)).thenReturn(1);
        when(productMapper.selectByIdWithDefaultStock(7L)).thenReturn(persisted);
        ProductStatusRequest request = new ProductStatusRequest();
        request.setStatus(ProductStatus.ON_SHELF);
        request.setVersion(4);

        Product updated = service.changeStatus(7L, request, admin);

        verify(productMapper).updateStatus(7L, ProductStatus.ON_SHELF, 4);
        assertEquals(ProductStatus.ON_SHELF, updated.getStatus());
        assertEquals(10, updated.getAvailableStock());
        assertEquals(2, updated.getLockedStock());
        assertEquals(3, updated.getSoldStock());
        assertEquals(5, updated.getVersion());
    }

    @Test
    void stockAdjustmentOnlyChangesAvailableStockAndWritesBeforeAfterLedger() {
        Product locked = product(7L, ProductStatus.ON_SHELF, 10, 2, 3, 4);
        when(productMapper.selectByIdForUpdate(7L)).thenReturn(locked);
        when(productSkuMapper.selectDefaultByProductId(7L)).thenReturn(sku(7L, 70L));
        when(skuStockMapper.selectBySkuIdForUpdate(70L)).thenReturn(stock(70L, 10, 2, 6L));
        when(operationMapper.selectByBusinessKey("ADMIN_ADJUST:req-1:7")).thenReturn(null);
        when(skuStockMapper.updateAvailableStock(70L, 15, 6L)).thenReturn(1);

        InventoryOperation operation = service.adjustStock(
                7L,
                stockRequest("req-1", 5, "补货"),
                admin
        );

        verify(skuStockMapper).updateAvailableStock(70L, 15, 6L);
        assertEquals(70L, operation.getSkuId());
        assertEquals(5, operation.getAvailableDelta());
        assertEquals(0, operation.getLockedDelta());
        assertEquals(0, operation.getSoldDelta());
        assertEquals(10, operation.getAvailableBefore());
        assertEquals(15, operation.getAvailableAfter());
        assertEquals(2, operation.getLockedBefore());
        assertEquals(2, operation.getLockedAfter());
        assertEquals(0, operation.getSoldBefore());
        assertEquals(0, operation.getSoldAfter());
        verify(operationMapper).insert(operation);
    }

    @Test
    void negativeAdjustmentCannotMakeAvailableStockNegative() {
        when(productMapper.selectByIdForUpdate(7L))
                .thenReturn(product(7L, ProductStatus.ON_SHELF, 3, 2, 3, 4));
        when(productSkuMapper.selectDefaultByProductId(7L)).thenReturn(sku(7L, 70L));
        when(skuStockMapper.selectBySkuIdForUpdate(70L)).thenReturn(stock(70L, 3, 2, 6L));
        when(operationMapper.selectByBusinessKey("ADMIN_ADJUST:req-2:7")).thenReturn(null);

        assertThrows(ConflictException.class,
                () -> service.adjustStock(7L, stockRequest("req-2", -4, "盘亏"), admin));

        verify(skuStockMapper, never()).updateAvailableStock(any(), any(), any());
        verify(operationMapper, never()).insert(any(InventoryOperation.class));
    }

    @Test
    void stockAdjustmentRequiresTheProductsDefaultSku() {
        when(productMapper.selectByIdForUpdate(7L))
                .thenReturn(product(7L, ProductStatus.ON_SHELF, 3, 2, 0, 4));
        when(productSkuMapper.selectDefaultByProductId(7L)).thenReturn(null);

        assertThrows(ConflictException.class,
                () -> service.adjustStock(7L, stockRequest("req-no-sku", 1, "补货"), admin));

        verify(skuStockMapper, never()).updateAvailableStock(any(), any(), any());
        verify(operationMapper, never()).insert(any(InventoryOperation.class));
    }

    @Test
    void duplicateStockAdjustmentWithSamePayloadIsANoOp() {
        Product locked = product(7L, ProductStatus.ON_SHELF, 15, 2, 3, 5);
        InventoryOperation existing = existingOperation(7L, 5, "补货");
        when(productMapper.selectByIdForUpdate(7L)).thenReturn(locked);
        when(productSkuMapper.selectDefaultByProductId(7L)).thenReturn(sku(7L, 70L));
        when(operationMapper.selectByBusinessKey("ADMIN_ADJUST:req-1:7")).thenReturn(existing);

        InventoryOperation replayed = service.adjustStock(
                7L,
                stockRequest("req-1", 5, "补货"),
                admin
        );

        assertSame(existing, replayed);
        verify(skuStockMapper, never()).updateAvailableStock(any(), any(), any());
        verify(operationMapper, never()).insert(any(InventoryOperation.class));
    }

    @Test
    void duplicateStockAdjustmentWithDifferentPayloadIsRejected() {
        Product locked = product(7L, ProductStatus.ON_SHELF, 15, 2, 3, 5);
        when(productMapper.selectByIdForUpdate(7L)).thenReturn(locked);
        when(productSkuMapper.selectDefaultByProductId(7L)).thenReturn(sku(7L, 70L));
        when(operationMapper.selectByBusinessKey("ADMIN_ADJUST:req-1:7"))
                .thenReturn(existingOperation(7L, 5, "补货"));

        assertThrows(ConflictException.class,
                () -> service.adjustStock(7L, stockRequest("req-1", 6, "补货"), admin));

        verify(skuStockMapper, never()).updateAvailableStock(any(), any(), any());
        verify(operationMapper, never()).insert(any(InventoryOperation.class));
    }

    @Test
    void missingProductUsesNotFoundSemantics() {
        when(productMapper.selectByIdForUpdate(404L)).thenReturn(null);

        assertThrows(NotFoundException.class,
                () -> service.update(404L, updateRequest("不存在", 1, 0), admin));
    }

    @Test
    void adminListAndOperationListDelegateWithoutFilteringStatuses() {
        Product onShelf = product(1L, ProductStatus.ON_SHELF, 1, 2, 3, 4);
        Product offShelf = product(2L, ProductStatus.OFF_SHELF, 5, 6, 7, 8);
        InventoryOperation operation = existingOperation(1L, 1, "补货");
        when(productMapper.selectAllWithDefaultStock()).thenReturn(Arrays.asList(onShelf, offShelf));
        when(productMapper.selectById(1L)).thenReturn(onShelf);
        when(operationMapper.selectByProductIdOrderByCreateTimeDesc(1L))
                .thenReturn(Collections.singletonList(operation));

        assertEquals(Arrays.asList(onShelf, offShelf), service.listAdmin());
        assertEquals(Collections.singletonList(operation), service.listOperations(1L));
        verify(productMapper).selectAllWithDefaultStock();
    }

    @Test
    void operationListForMissingProductUsesNotFoundSemantics() {
        when(productMapper.selectById(404L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> service.listOperations(404L));

        verify(operationMapper, never()).selectByProductIdOrderByCreateTimeDesc(404L);
    }

    @Test
    void productAdminContractHasNoDeleteOperation() {
        assertFalse(Arrays.stream(ProductAdminService.class.getMethods())
                .anyMatch(method -> method.getName().toLowerCase().contains("delete")));
    }

    private ProductUpdateRequest updateRequest(String title, int price, int version) {
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setTitle(title);
        request.setPrice(price);
        request.setVersion(version);
        return request;
    }

    private StockAdjustmentRequest stockRequest(String requestId, int delta, String reason) {
        StockAdjustmentRequest request = new StockAdjustmentRequest();
        request.setRequestId(requestId);
        request.setDelta(delta);
        request.setReason(reason);
        return request;
    }

    private Product product(
            Long id,
            ProductStatus status,
            int available,
            int locked,
            int sold,
            int version
    ) {
        Product product = new Product();
        product.setId(id);
        product.setTitle("商品" + id);
        product.setPrice(1000);
        product.setStatus(status);
        product.setSkuId(id * 10);
        product.setSkuStatus("ACTIVE");
        product.setAvailableStock(available);
        product.setLockedStock(locked);
        product.setSoldStock(sold);
        product.setVersion(version);
        return product;
    }

    private InventoryOperation existingOperation(Long productId, int delta, String reason) {
        InventoryOperation operation = new InventoryOperation();
        operation.setProductId(productId);
        operation.setSkuId(productId * 10);
        operation.setAvailableDelta(delta);
        operation.setReason(reason);
        return operation;
    }

    private ProductSku sku(Long productId, Long skuId) {
        ProductSku sku = new ProductSku();
        sku.setId(skuId);
        sku.setProductId(productId);
        sku.setStatus("ACTIVE");
        return sku;
    }

    private SkuStock stock(Long skuId, int available, int locked, long version) {
        SkuStock stock = new SkuStock();
        stock.setSkuId(skuId);
        stock.setAvailableStock(available);
        stock.setLockedStock(locked);
        stock.setVersion(version);
        return stock;
    }
}
