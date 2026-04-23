package cc.ivera.policy;

import cc.ivera.pojo.ServerTableData;
import com.deepoove.poi.data.RowRenderData;
import com.deepoove.poi.policy.DynamicTableRenderPolicy;
import com.deepoove.poi.policy.TableRenderPolicy;
import com.deepoove.poi.util.TableTools;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.util.List;
import java.util.Map;

public class ServerTablePolicy extends DynamicTableRenderPolicy {
    @Override
    public void render(XWPFTable xwpfTable, Object tableData) throws Exception {
        if (null == tableData) {
            return;
        }
 
        // 参数数据声明
        ServerTableData serverTableData = (ServerTableData) tableData;
        List<RowRenderData> serverDataList = serverTableData.getServerDataList();
        List<Map<String, Object>> groupDataList = serverTableData.getGroupDataList();
        Integer mergeColumn = serverTableData.getMergeColumn();
 
        if (CollectionUtils.isNotEmpty(serverDataList)) {
            // 先删除一行, demo中第一行是为了调整 三线表 样式
            xwpfTable.removeRow(1);
 
            // 行从中间插入, 因此采用倒序渲染数据
            for (int i = serverDataList.size() - 1; i >= 0; i--) {
                XWPFTableRow newRow = xwpfTable.insertNewTableRow(1);
                newRow.setHeight(400);
                //每一行填充多少个单元格
                for (int j = 0; j < serverDataList.get(0).getCells().size(); j++) {
                    newRow.createCell();
                }
                // 渲染一行数据
                TableRenderPolicy.Helper.renderRow(newRow, serverDataList.get(i));
            }
 
 
            // 处理合并
            for (int i = 0; i < serverDataList.size(); i++) {
                // 获取要合并的名称那一列数据 mergeColumn代表要合并的列，从0开始
                String typeNameData = serverDataList.get(i).getCells().get(mergeColumn).getParagraphs().get(0).getContents().get(0).toString();
                for (int j = 0; j < groupDataList.size(); j++) {
                    String typeNameTemplate = String.valueOf(groupDataList.get(j).get("typeName"));
                    int listSize = Integer.parseInt(String.valueOf(groupDataList.get(j).get("listSize")));
                    //如果只有一条数据不进行合并处理
                    if (listSize!=1) {
                        // 若匹配上 就直接合并
                        if (typeNameTemplate.equals(typeNameData)) {
                            //如果合并列不为空直接合并,否则提示输入要合并的列
                            if (CollectionUtils.isNotEmpty(serverTableData.getMergeColumnList())) {
                                for (Integer mergeColumns : serverTableData.getMergeColumnList()) {
                                    TableTools.mergeCellsVertically(xwpfTable, mergeColumns, i + 1, i + listSize);
                                }
                                groupDataList.remove(j);
                                break;
                            } else {
                                throw new Exception("要合并列不能为空!");
                            }
                        }
                    }
                }
            }
        }
    }
}