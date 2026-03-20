package cc.ivera.mapper;

import cc.ivera.bean.Class;

import java.util.List;

public interface One2ManyMapper {
    /**
     * 查询全部
     * @return
     */
    List<Class> selectAll();

    /**
     * 查询全部
     * @return
     */
    List<Class> selectAll2();
}
