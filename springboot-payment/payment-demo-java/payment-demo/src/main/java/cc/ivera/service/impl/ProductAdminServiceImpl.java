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
import cc.ivera.exception.BizException;
import cc.ivera.exception.ConflictException;
import cc.ivera.exception.NotFoundException;
import cc.ivera.mapper.InventoryOperationMapper;
import cc.ivera.mapper.ProductMapper;
import cc.ivera.mapper.ProductSkuMapper;
import cc.ivera.mapper.SkuStockMapper;
import cc.ivera.security.AuthUser;
import cc.ivera.service.ProductAdminService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Service
public class ProductAdminServiceImpl implements ProductAdminService {

    private static final String ADMIN_ADJUST = "ADMIN_ADJUST";

    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final SkuStockMapper skuStockMapper;
    private final InventoryOperationMapper operationMapper;

    public ProductAdminServiceImpl(
            ProductMapper productMapper,
            ProductSkuMapper productSkuMapper,
            SkuStockMapper skuStockMapper,
            InventoryOperationMapper operationMapper
    ) {
        this.productMapper = productMapper;
        this.productSkuMapper = productSkuMapper;
        this.skuStockMapper = skuStockMapper;
        this.operationMapper = operationMapper;
    }

    @Override
    public List<Product> listAdmin() {
        return productMapper.selectAllWithDefaultStock();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Product create(ProductCreateRequest request, AuthUser operator) {
        int initialStock = request.getInitialStock() == null ? 0 : request.getInitialStock();
        if (initialStock < 0) {
            throw new BizException("初始库存不能小于0");
        }
        Product product = new Product();
        product.setTitle(request.getTitle());
        product.setPrice(request.getPrice());
        product.setStatus(ProductStatus.OFF_SHELF);
        product.setVersion(0);
        if (productMapper.insert(product) != 1) {
            throw new BizException("商品创建失败");
        }
        ProductSku sku = new ProductSku();
        sku.setProductId(product.getId());
        sku.setSkuCode("DEFAULT-" + product.getId());
        sku.setSkuName("默认规格");
        sku.setSkuSpec("{}");
        sku.setStatus("ACTIVE");
        if (productSkuMapper.insert(sku) != 1 || sku.getId() == null) {
            throw new BizException("默认SKU创建失败");
        }
        SkuStock stock = new SkuStock();
        stock.setSkuId(sku.getId());
        stock.setAvailableStock(initialStock);
        stock.setLockedStock(0);
        stock.setVersion(0L);
        if (skuStockMapper.insert(stock) != 1) {
            throw new BizException("默认SKU库存创建失败");
        }
        if (initialStock > 0) {
            InventoryOperation operation = inventoryOperation(
                    "ADMIN_ADJUST:__CREATE__:" + product.getId(),
                    product,
                    sku.getId(),
                    initialStock,
                    0,
                    initialStock,
                    0,
                    operator,
                    "新建商品初始库存"
            );
            if (operationMapper.insert(operation) != 1) {
                throw new BizException("初始库存流水写入失败");
            }
        }
        return requirePersistedProduct(product.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Product update(Long id, ProductUpdateRequest request, AuthUser operator) {
        Product product = requireLockedProduct(id);
        requireCurrentVersion(product, request.getVersion());
        if (productMapper.updateBasicInfo(id, request.getTitle(), request.getPrice(), request.getVersion()) != 1) {
            throw new ConflictException("商品版本已变化，请刷新后重试");
        }
        return requirePersistedProduct(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Product changeStatus(Long id, ProductStatusRequest request, AuthUser operator) {
        Product product = requireLockedProduct(id);
        requireCurrentVersion(product, request.getVersion());
        if (productMapper.updateStatus(id, request.getStatus(), request.getVersion()) != 1) {
            throw new ConflictException("商品版本已变化，请刷新后重试");
        }
        return requirePersistedProduct(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryOperation adjustStock(
            Long id,
            StockAdjustmentRequest request,
            AuthUser operator
    ) {
        requireAdjustmentRequest(request);
        Product product = requireLockedProduct(id);
        ProductSku sku = requireDefaultSku(id);
        String businessKey = "ADMIN_ADJUST:" + request.getRequestId() + ":" + id;
        InventoryOperation existing = operationMapper.selectByBusinessKey(businessKey);
        if (existing != null) {
            if (Objects.equals(existing.getProductId(), id)
                    && Objects.equals(existing.getSkuId(), sku.getId())
                    && Objects.equals(existing.getAvailableDelta(), request.getDelta())
                    && Objects.equals(existing.getReason(), request.getReason())) {
                return existing;
            }
            throw new ConflictException("库存调整请求号已被不同参数使用");
        }

        SkuStock stock = skuStockMapper.selectBySkuIdForUpdate(sku.getId());
        if (stock == null
                || stock.getAvailableStock() == null
                || stock.getAvailableStock() < 0
                || stock.getLockedStock() == null
                || stock.getLockedStock() < 0
                || stock.getVersion() == null
                || stock.getVersion() < 0) {
            throw new ConflictException("默认SKU库存记录不存在或余额非法");
        }
        int availableBefore = stock.getAvailableStock();
        long availableAfterValue = (long) availableBefore + request.getDelta();
        if (availableAfterValue < 0) {
            throw new ConflictException("商品可用库存不足");
        }
        if (availableAfterValue > Integer.MAX_VALUE) {
            throw new ConflictException("商品库存数量超出范围");
        }
        int availableAfter = (int) availableAfterValue;
        if (skuStockMapper.updateAvailableStock(
                sku.getId(), availableAfter, stock.getVersion()) != 1) {
            throw new ConflictException("SKU库存版本已变化，请刷新后重试");
        }

        InventoryOperation operation = inventoryOperation(
                businessKey,
                product,
                sku.getId(),
                request.getDelta(),
                availableBefore,
                availableAfter,
                stock.getLockedStock(),
                operator,
                request.getReason()
        );
        if (operationMapper.insert(operation) != 1) {
            throw new BizException("库存流水写入失败");
        }
        return operation;
    }

    @Override
    public List<InventoryOperation> listOperations(Long id) {
        if (productMapper.selectById(id) == null) {
            throw new NotFoundException("商品不存在");
        }
        return operationMapper.selectByProductIdOrderByCreateTimeDesc(id);
    }

    private Product requireLockedProduct(Long id) {
        Product product = productMapper.selectByIdForUpdate(id);
        if (product == null) {
            throw new NotFoundException("商品不存在");
        }
        return product;
    }

    private Product requirePersistedProduct(Long id) {
        Product product = productMapper.selectByIdWithDefaultStock(id);
        if (product == null) {
            throw new NotFoundException("商品不存在");
        }
        return product;
    }

    private ProductSku requireDefaultSku(Long productId) {
        ProductSku sku = productSkuMapper.selectDefaultByProductId(productId);
        if (sku == null || sku.getId() == null || !"ACTIVE".equals(sku.getStatus())) {
            throw new ConflictException("商品默认SKU不存在或不可用");
        }
        return sku;
    }

    private void requireCurrentVersion(Product product, Integer requestedVersion) {
        if (!Objects.equals(product.getVersion(), requestedVersion)) {
            throw new ConflictException("商品版本已变化，请刷新后重试");
        }
    }

    private void requireAdjustmentRequest(StockAdjustmentRequest request) {
        if (request == null
                || !StringUtils.hasText(request.getRequestId())
                || request.getDelta() == null
                || request.getDelta() == 0
                || !StringUtils.hasText(request.getReason())) {
            throw new BizException("库存调整参数不合法");
        }
    }

    private InventoryOperation inventoryOperation(
            String businessKey,
            Product product,
            Long skuId,
            int availableDelta,
            int availableBefore,
            int availableAfter,
            int lockedStock,
            AuthUser operator,
            String reason
    ) {
        InventoryOperation operation = new InventoryOperation();
        operation.setBusinessKey(businessKey);
        operation.setProductId(product.getId());
        operation.setSkuId(skuId);
        operation.setOperationType(ADMIN_ADJUST);
        operation.setAvailableDelta(availableDelta);
        operation.setLockedDelta(0);
        operation.setSoldDelta(0);
        operation.setAvailableBefore(availableBefore);
        operation.setAvailableAfter(availableAfter);
        operation.setLockedBefore(lockedStock);
        operation.setLockedAfter(lockedStock);
        operation.setSoldBefore(0);
        operation.setSoldAfter(0);
        operation.setOperatorId(operator == null ? null : operator.getUserId());
        operation.setOperatorName(operator == null ? null : operator.getUsername());
        operation.setReason(reason);
        return operation;
    }
}
