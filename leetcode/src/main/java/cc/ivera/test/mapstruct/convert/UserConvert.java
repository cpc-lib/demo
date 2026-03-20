package cc.ivera.test.mapstruct.convert;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;
import cc.ivera.test.mapstruct.bo.UserBO;
import cc.ivera.test.mapstruct.bo.UserDetailBO;
import cc.ivera.test.mapstruct.dataobject.UserDO;

@Mapper
public interface UserConvert {

    UserConvert INSTANCE = Mappers.getMapper(UserConvert.class);

    UserBO convert(UserDO userDO);

    @Mappings({
            @Mapping(source = "id", target = "userId")
    })
    UserDetailBO convertDetail(UserDO userDO);

}
