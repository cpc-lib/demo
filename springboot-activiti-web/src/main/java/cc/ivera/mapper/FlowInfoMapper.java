package cc.ivera.mapper;

import cc.ivera.entity.FlowInfo;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface FlowInfoMapper {

    @Select("select * from tb_flow_info order by create_time desc")
    List<FlowInfo> selectFlowList();

    @Update("update tb_flow_info set state = 0 where id=#{id}")
    int updateFlowDeployState(Long id);

    @Select("select * from tb_flow_info where id=#{id}")
    FlowInfo selectOneFlow(Long id);
}
