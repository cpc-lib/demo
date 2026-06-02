package cc.ivera.controller;

import cc.ivera.entity.Product;
import cc.ivera.service.ProductService;
import cc.ivera.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;
import java.util.Map;

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

        List<Product> list = productService.list();
        return R.ok().data("productList", list);
    }

}
