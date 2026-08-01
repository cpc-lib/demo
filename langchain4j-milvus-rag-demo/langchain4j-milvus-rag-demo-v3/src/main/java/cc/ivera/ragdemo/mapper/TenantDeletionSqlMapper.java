package cc.ivera.ragdemo.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface TenantDeletionSqlMapper {

    long countTenantRows(@Param("table") String table, @Param("tenantId") Long tenantId);

    int deleteTenantRows(@Param("table") String table, @Param("tenantId") Long tenantId);
}
