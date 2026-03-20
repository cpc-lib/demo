package cc.ivera.mapper;

import cc.ivera.bean.Student;

import java.util.List;

public interface Many2ManyMapper {
    /**
     * 查询全部
     * @return
     */
    List<Student> selectAll();
}
