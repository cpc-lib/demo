package cc.ivera.mapper;

import cc.ivera.entity.Evection;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface EvectionMapper {
    @Select("select * from tb_evection where user_id = #{userId}")
    List<Evection> selectAll(Long userId);

    @Update("update tb_evection set state=1 where id=#{id}")
    int startTask(Long id);

    @Update("update tb_evection set state=2 where id=#{id}")
    int endTask(Long id);

    @Select("select * from tb_evection where id=#{id}")
    Evection selectOne(Long id);

    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    @Insert("insert into tb_evection (user_id,evection_name,num,begin_date,end_date,destination,reason,state) values (#{user_id},#{evection_name},#{num},#{begin_date},#{end_date},#{destination},#{reason},#{state})")
    int save(Evection evection);
}
