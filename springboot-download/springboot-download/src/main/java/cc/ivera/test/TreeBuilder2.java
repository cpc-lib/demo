package cc.ivera.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TreeBuilder2 {

    public static void main(String[] args) {
        List<Map<String, Object>> dataList = getDataFromDatabase(); // 从数据库获取数据
        List<TreeNode2> tree = buildTree(dataList);
        // 现在tree就是整个树的根节点列表，你可以进一步处理它或输出树结构
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

    private static List<TreeNode2> buildTree(List<Map<String, Object>> dataList) {
        Map<String, TreeNode2> nodeMap = new HashMap<>();
        List<TreeNode2> roots = new ArrayList<>();

        // 第一遍遍历：将数据添加到节点映射表中
        for (Map<String, Object> data : dataList) {
            String id = (String) data.get("id");
            String parentId = (String) data.get("parent_id");

            TreeNode2 node = new TreeNode2(id, parentId, 1);
            nodeMap.put(id, node);

            if (parentId == null) {
                roots.add(node);
            }
        }

        // 第二遍遍历：构建树结构
        for (TreeNode2 node : nodeMap.values()) {
            String parentId = node.getParentId();
            if (parentId != null) {
                TreeNode2 parent = nodeMap.get(parentId);
                if (parent != null) {
                    node.setLevel(parent.getLevel() + 1);
                    parent.addChild(node);
                }
            }
        }

        return roots;
    }
}
