package cc.ivera.test.tree;

import cc.ivera.util.TreeBuilder2;
import cc.ivera.util.TreeNode2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Demo3 {

    public static void main(String[] args) {
        List<Map<String, Object>> dataList = getDataFromDatabase(); // 从数据库获取数据
        List<TreeNode2> tree = TreeBuilder2.buildTree(dataList);
        // 现在tree就是整个树的根节点列表，你可以进一步处理它或输出树结构
        for (TreeNode2 treeNode2 : tree) {
            System.out.println(treeNode2);
        }
    }

    private static List<Map<String, Object>> getDataFromDatabase() {
        // 模拟从数据库中获取数据
        List<Map<String, Object>> dataList = new ArrayList<>();

        Map<String, Object> data1 = new HashMap<>();
        data1.put("id", "1001");
        data1.put("parent_id", null);
        dataList.add(data1);

        Map<String, Object> data2 = new HashMap<>();
        data2.put("id", "1002");
        data2.put("parent_id", "1001");
        dataList.add(data2);

        return dataList;
    }
}
