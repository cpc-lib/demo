package cc.ivera.item.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cc.ivera.item.mapper.ItemStockMapper;
import cc.ivera.item.pojo.ItemStock;
import cc.ivera.item.service.IItemStockService;
import org.springframework.stereotype.Service;

@Service
public class ItemStockService extends ServiceImpl<ItemStockMapper, ItemStock> implements IItemStockService {
}
