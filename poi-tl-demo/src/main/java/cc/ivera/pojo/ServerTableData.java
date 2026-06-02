package cc.ivera.pojo;

import com.deepoove.poi.data.RowRenderData;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ServerTableData {
 
    /**
     *  携带表格中真实数据
     */
    private List<RowRenderData> serverDataList;
 
    /**
     * 携带要分组的信息
     */
    private List<Map<String, Object>> groupDataList;
 
    /**
     * 需要合并的列，从0开始
     */
    private Integer mergeColumn;
 
    /**
     * 具体要合并的列集合
     */
    private List<Integer> mergeColumnList;
}