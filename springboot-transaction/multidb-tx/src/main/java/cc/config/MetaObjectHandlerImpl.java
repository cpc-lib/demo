package cc.ivera.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Date;


/**
 * 实现MetaObjectHandler接口实现时间的自动填充
 */
@Component
@Order(1)
public class MetaObjectHandlerImpl implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        Object created = getFieldValByName("created", metaObject);
        if (null == created) {
            //字段为空，可以进行填充
            setFieldValByName("created", new Date(), metaObject);
        }


        Object enabled = getFieldValByName("enabled", metaObject);
        if (null == enabled) {
            //设置逻辑删除字段的默认值
            setFieldValByName("enabled", "1", metaObject);
        }

        Object updated = getFieldValByName("updated", metaObject);
        if (null == updated) {
            //字段为空，可以进行填充
            setFieldValByName("updated", new Date(), metaObject);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        //更新数据时，直接更新字段
        setFieldValByName("updated", new Date(), metaObject);
    }
}
