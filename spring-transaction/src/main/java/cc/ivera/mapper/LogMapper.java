package cc.ivera.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.Date;

public interface LogMapper {
    void logInfo(@Param("outName")String outName,
                 @Param("inName")String inName,
                 @Param("money")Integer money,
                 @Param("date")String date,
                 @Param("status")Boolean status);
}
