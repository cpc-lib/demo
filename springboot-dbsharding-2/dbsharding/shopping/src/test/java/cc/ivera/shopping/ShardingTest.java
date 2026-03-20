package cc.ivera.shopping;

import cc.ivera.shopping.dao.ProductDao;
import cc.ivera.shopping.entity.ProductInfo;
import cc.ivera.shopping.service.ProductService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author Administrator
 * @version 1.0
 **/
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ShoppingBootstrap.class)
public class ShardingTest {

    @Autowired
    ProductService productService;

    @Autowired
    ProductDao productDao;

    //添加商品
    @Test
    public void testCreateProduct(){
        for (int i=1;i<10;i++){
            ProductInfo productInfo = new ProductInfo();
            productInfo.setStoreInfoId(1L);//店铺id
            productInfo.setProductName("日月神话"+i);//商品名称
            productInfo.setSpec("大号");
            productInfo.setPrice(new BigDecimal(60));
            productInfo.setRegionCode("110100");
            productInfo.setDescript("日月神话不错！！！"+i);//商品描述
            productService.createProduct(productInfo);
        }

    }

    //查询商品
    @Test
    public void testQueryProduct(){

        List<ProductInfo> productInfos = productService.queryProduct(1, 20);
        System.out.println(productInfos.size());
    }

    //统计商品总数
    @Test
    public void testSelectCount(){

        int i = productDao.selectCount();

        System.out.println(i);
    }

    //分组统计商品
    @Test
    public void testSelectProductGroupList(){

        List<Map> maps = productDao.selectProductGroupList();

        System.out.println(maps);
    }

}
