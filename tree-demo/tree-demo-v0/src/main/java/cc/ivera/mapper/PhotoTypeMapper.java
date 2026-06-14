
package cc.ivera.mapper;

import cc.ivera.entity.PhotoType;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface PhotoTypeMapper extends BaseMapper<PhotoType> {
    List<PhotoType> lockByParent(String parentId);

    int batchUpdateSort(@Param("list") List<PhotoType> list);
}
