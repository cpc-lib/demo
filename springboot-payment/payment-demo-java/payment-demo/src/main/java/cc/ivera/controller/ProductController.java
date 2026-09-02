package cc.ivera.controller;

import cc.ivera.entity.Product;
import cc.ivera.enums.ProductStatus;
import cc.ivera.service.ProductService;
import cc.ivera.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin //开放前端的跨域访问
@Api(tags = "商品管理")
@RestController
@RequestMapping("/api/product")
public class ProductController {

    private final ProductService productService;

    public ProductController(
        ProductService productService
    ) {
        this.productService = productService;
    }

    @ApiOperation("测试接口")
    @GetMapping("/test")
    public R<Map<String, Object>> test() {
        return R.ok().data("message", "hello").data("now", new Date());
    }

    @ApiOperation("商品列表")
    @GetMapping("/list")
    public R<Map<String, Object>> list() {
        List<Map<String, Object>> list = productService.listPublicSaleable().stream()
                .map(this::toPublicView)
                .collect(Collectors.toList());
        return R.ok().data("productList", list);
    }

    private Map<String, Object> toPublicView(Product product) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", product.getId());
        view.put("title", product.getTitle());
        view.put("price", product.getPrice());
        view.put("availableStock", product.getAvailableStock());
        view.put("saleable", product.getStatus() == ProductStatus.ON_SHELF
                && product.getAvailableStock() != null
                && product.getAvailableStock() > 0);
        return view;
    }
}
