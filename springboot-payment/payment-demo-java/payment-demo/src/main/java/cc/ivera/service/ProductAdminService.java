package cc.ivera.service;

import cc.ivera.dto.ProductCreateRequest;
import cc.ivera.dto.ProductStatusRequest;
import cc.ivera.dto.ProductUpdateRequest;
import cc.ivera.dto.StockAdjustmentRequest;
import cc.ivera.entity.InventoryOperation;
import cc.ivera.entity.Product;
import cc.ivera.security.AuthUser;

import java.util.List;

public interface ProductAdminService {

    List<Product> listAdmin();

    Product create(ProductCreateRequest request, AuthUser operator);

    Product update(Long id, ProductUpdateRequest request, AuthUser operator);

    Product changeStatus(Long id, ProductStatusRequest request, AuthUser operator);

    InventoryOperation adjustStock(Long id, StockAdjustmentRequest request, AuthUser operator);

    List<InventoryOperation> listOperations(Long id);
}
