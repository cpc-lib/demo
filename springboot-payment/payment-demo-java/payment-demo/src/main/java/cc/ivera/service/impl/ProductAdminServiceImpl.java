package cc.ivera.service.impl;

import cc.ivera.dto.ProductCreateRequest;
import cc.ivera.dto.ProductStatusRequest;
import cc.ivera.dto.ProductUpdateRequest;
import cc.ivera.dto.StockAdjustmentRequest;
import cc.ivera.entity.InventoryOperation;
import cc.ivera.entity.Product;
import cc.ivera.enums.ProductStatus;
import cc.ivera.exception.BizException;
import cc.ivera.exception.ConflictException;
import cc.ivera.exception.NotFoundException;
import cc.ivera.mapper.InventoryOperationMapper;
import cc.ivera.mapper.ProductMapper;
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
    private final InventoryOperationMapper operationMapper;

    public ProductAdminServiceImpl(
            ProductMapper productMapper,
            InventoryOperationMapper operationMapper
    ) {
        this.productMapper = productMapper;
        this.operationMapper = operationMapper;
    }

    @Override
    public List<Product> listAdmin() {
        return productMapper.selectList(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Product create(ProductCreateRequest request, AuthUser operator) {
        Product product = new Product();
        product.setTitle(request.getTitle());
        product.setPrice(request.getPrice());
        product.setStatus(ProductStatus.OFF_SHELF);
        product.setAvailableStock(request.getInitialStock() == null ? 0 : request.getInitialStock());
        product.setLockedStock(0);
        product.setSoldStock(0);
        product.setVersion(0);
        if (productMapper.insert(product) != 1) {
            throw new BizException("商品创建失败");
        }
        if (product.getAvailableStock() > 0) {
            InventoryOperation operation = inventoryOperation(
                    "ADMIN_ADJUST:__CREATE__:" + product.getId(),
                    product,
                    product.getAvailableStock(),
                    0,
                    product.getAvailableStock(),
                    operator,
                    "新建商品初始库存"
            );
            operationMapper.insert(operation);
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
        String businessKey = "ADMIN_ADJUST:" + request.getRequestId() + ":" + id;
        InventoryOperation existing = operationMapper.selectByBusinessKey(businessKey);
        if (existing != null) {
            if (Objects.equals(existing.getProductId(), id)
                    && Objects.equals(existing.getAvailableDelta(), request.getDelta())
                    && Objects.equals(existing.getReason(), request.getReason())) {
                return existing;
            }
            throw new ConflictException("库存调整请求号已被不同参数使用");
        }

        int availableBefore = product.getAvailableStock() == null ? 0 : product.getAvailableStock();
        long availableAfterValue = (long) availableBefore + request.getDelta();
        if (availableAfterValue < 0) {
            throw new ConflictException("商品可用库存不足");
        }
        if (availableAfterValue > Integer.MAX_VALUE) {
            throw new ConflictException("商品库存数量超出范围");
        }
        int availableAfter = (int) availableAfterValue;
        if (productMapper.updateAvailableStock(id, availableAfter, product.getVersion()) != 1) {
            throw new ConflictException("商品版本已变化，请刷新后重试");
        }

        InventoryOperation operation = inventoryOperation(
                businessKey,
                product,
                request.getDelta(),
                availableBefore,
                availableAfter,
                operator,
                request.getReason()
        );
        operationMapper.insert(operation);
        product.setAvailableStock(availableAfter);
        product.setVersion(product.getVersion() + 1);
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
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new NotFoundException("商品不存在");
        }
        return product;
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
            int availableDelta,
            int availableBefore,
            int availableAfter,
            AuthUser operator,
            String reason
    ) {
        int lockedStock = product.getLockedStock() == null ? 0 : product.getLockedStock();
        int soldStock = product.getSoldStock() == null ? 0 : product.getSoldStock();
        InventoryOperation operation = new InventoryOperation();
        operation.setBusinessKey(businessKey);
        operation.setProductId(product.getId());
        operation.setOperationType(ADMIN_ADJUST);
        operation.setAvailableDelta(availableDelta);
        operation.setLockedDelta(0);
        operation.setSoldDelta(0);
        operation.setAvailableBefore(availableBefore);
        operation.setAvailableAfter(availableAfter);
        operation.setLockedBefore(lockedStock);
        operation.setLockedAfter(lockedStock);
        operation.setSoldBefore(soldStock);
        operation.setSoldAfter(soldStock);
        operation.setOperatorId(operator == null ? null : operator.getUserId());
        operation.setOperatorName(operator == null ? null : operator.getUsername());
        operation.setReason(reason);
        return operation;
    }
}
