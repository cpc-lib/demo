package cc.ivera.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import cc.ivera.entity.FileRecord;

@Mapper
public interface FileRecordMapper {
    void insertFileRecord(FileRecord fileRecord);
    FileRecord findByFileId(@Param("fileId") String fileId);
}
