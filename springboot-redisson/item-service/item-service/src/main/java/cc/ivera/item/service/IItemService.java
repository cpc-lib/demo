package cc.ivera.item.service;

import cc.ivera.item.pojo.Item;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IItemService extends IService<Item> {
    void saveItem(Item item);
}
