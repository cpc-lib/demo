package cc.ivera.mapper;

import cc.ivera.entity.SiteMessage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface SiteMessageMapper {

    @Select("select * from tb_site_message where user_id=#{user_id}")
    List<SiteMessage> selectMsgList(Long user_id);

    @Select("select * from tb_site_message where id=#{id} and userid=#{user_id}")
    SiteMessage selectOneMsg(@Param("id") Long id, @Param("user_id") Long user_id);

    @Update("update tb_site_message where id=#{id} and user_id=#{user_id}")
    int updateMsgRead(@Param("id") Long id, @Param("user_id") Long userid);

    @Insert("insert into tb_site_message (user_id,task_id,type,is_read) values (#{user_id},#{task_id},#{type},#{is_read})")
    int insertMsg(@Param("user_id") Long userId, @Param("task_id") String taskId, @Param("type") Integer type, @Param("is_read") Integer isread);
}
