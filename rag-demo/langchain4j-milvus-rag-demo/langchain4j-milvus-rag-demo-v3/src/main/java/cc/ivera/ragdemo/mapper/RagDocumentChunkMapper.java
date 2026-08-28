package cc.ivera.ragdemo.mapper;

import cc.ivera.ragdemo.domain.rag.RagDocumentChunk;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RagDocumentChunkMapper extends BaseMapper<RagDocumentChunk> {

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM rag_document_chunk ORDER BY chunk_uid ASC, chunk_version ASC")
    List<RagDocumentChunk> selectAllForRedisRebuild();
}
