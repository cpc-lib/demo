package cc.ivera.controller;

import cc.ivera.dto.ProductCreateRequest;
import cc.ivera.dto.ProductStatusRequest;
import cc.ivera.dto.ProductUpdateRequest;
import cc.ivera.dto.StockAdjustmentRequest;
import cc.ivera.entity.InventoryOperation;
import cc.ivera.security.AuthContext;
import cc.ivera.security.AuthUser;
import cc.ivera.service.ProductAdminService;
import cc.ivera.vo.ProductAdminView;
import cc.ivera.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Positive;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Api(tags = "管理员商品管理")
@RestController
@RequestMapping("/api/admin/products")
@Validated
public class AdminProductController {

    private final ProductAdminService productAdminService;

    public AdminProductController(ProductAdminService productAdminService) {
        this.productAdminService = productAdminService;
    }

    @ApiOperation("商品列表")
    @GetMapping
    public R<Map<String, Object>> list() {
        AuthContext.requireAdmin();
        List<ProductAdminView> list = productAdminService.listAdmin().stream()
                .map(ProductAdminView::from)
                .collect(Collectors.toList());
        return R.ok().data("list", list);
    }

    @ApiOperation("新增商品")
    @PostMapping
    public R<ProductAdminView> create(@Valid @RequestBody ProductCreateRequest request) {
        AuthUser admin = AuthContext.requireAdmin();
        return R.ok(ProductAdminView.from(productAdminService.create(request, admin)));
    }

    @ApiOperation("修改商品")
    @PutMapping("/{id}")
    public R<ProductAdminView> update(
            @PathVariable @Positive(message = "商品ID必须大于0") Long id,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        AuthUser admin = AuthContext.requireAdmin();
        return R.ok(ProductAdminView.from(productAdminService.update(id, request, admin)));
    }

    @ApiOperation("商品上下架")
    @PatchMapping("/{id}/status")
    public R<ProductAdminView> changeStatus(
            @PathVariable @Positive(message = "商品ID必须大于0") Long id,
            @Valid @RequestBody ProductStatusRequest request
    ) {
        AuthUser admin = AuthContext.requireAdmin();
        return R.ok(ProductAdminView.from(productAdminService.changeStatus(id, request, admin)));
    }

    @ApiOperation("调整商品库存")
    @PostMapping("/{id}/stock-adjustments")
    public R<InventoryOperation> adjustStock(
            @PathVariable @Positive(message = "商品ID必须大于0") Long id,
            @Valid @RequestBody StockAdjustmentRequest request
    ) {
        AuthUser admin = AuthContext.requireAdmin();
        return R.ok(productAdminService.adjustStock(id, request, admin));
    }

    @ApiOperation("商品库存流水")
    @GetMapping("/{id}/stock-operations")
    public R<List<InventoryOperation>> listOperations(
            @PathVariable @Positive(message = "商品ID必须大于0") Long id
    ) {
        AuthContext.requireAdmin();
        return R.ok(productAdminService.listOperations(id));
    }
}
