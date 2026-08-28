package cc.ivera.ragdemo.mapper;

import cc.ivera.ragdemo.domain.tenant.SysOperationAuditLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysOperationAuditLogMapper extends BaseMapper<SysOperationAuditLog> {
}
