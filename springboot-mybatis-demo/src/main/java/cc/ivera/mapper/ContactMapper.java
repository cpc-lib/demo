package cc.ivera.mapper;

import cc.ivera.entity.Contact;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ContactMapper {

    @Select("select * from tb_contact")
    public List<Contact> list();

    @Select("select * from tb_contact where id=#{id}")
    public Contact get(Integer id);

    @Insert("insert into tb_contact (name,telephone) values(#{name},#{telephone})")
    public void add(Contact contact);

    @Update("update tb_contact set name=#{name},telephone=#{telephone} where id=#{id}")
    public void update(Contact contact);

    @Delete("delete from tb_contact where id=#{id}")
    public void delete(Integer id);

}
