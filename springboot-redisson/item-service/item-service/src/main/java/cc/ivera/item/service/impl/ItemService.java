package cc.ivera.item.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cc.ivera.item.mapper.ItemMapper;
import cc.ivera.item.pojo.Item;
import cc.ivera.item.pojo.ItemStock;
import cc.ivera.item.service.IItemService;
import cc.ivera.item.service.IItemStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemService extends ServiceImpl<ItemMapper, Item> implements IItemService {
    @Autowired
    private IItemStockService stockService;

    @Override
    @Transactional
    public void saveItem(Item item) {
        // 新增商品
        save(item);
        // 新增库存
        ItemStock stock = new ItemStock();
        stock.setId(item.getId());
        stock.setStock(item.getStock());
        stockService.save(stock);
    }
}
