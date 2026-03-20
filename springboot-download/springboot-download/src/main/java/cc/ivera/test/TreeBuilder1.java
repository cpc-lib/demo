package cc.ivera.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TreeBuilder1 {

    public static void main(String[] args) {
        List<Map<String, Object>> dataList = getDataFromDatabase(); // 从数据库获取数据
        List<TreeNode1> tree = buildTree(dataList);
        // 现在tree就是整个树的根节点列表，你可以进一步处理它或输出树结构
    }

    private static List<Map<String, Object>> getDataFromDatabase() {
        // 模拟从数据库中获取数据
        List<Map<String, Object>> dataList = new ArrayList<>();

        Map<String, Object> data1 = new HashMap<>();
        data1.put("type_id", "1001");
        data1.put("level", 1);
        dataList.add(data1);

        Map<String, Object> data2 = new HashMap<>();
        data2.put("type_id", "10011001");
        data2.put("level", 2);
        dataList.add(data2);

        Map<String, Object> data3 = new HashMap<>();
        data3.put("type_id", "1002");
        data3.put("level", 1);
        dataList.add(data3);

        Map<String, Object> data4 = new HashMap<>();
        data4.put("type_id", "100110011001");
        data4.put("level", 3);
        dataList.add(data4);

        Map<String, Object> data5 = new HashMap<>();
        data5.put("type_id", "1001100110011001");
        data5.put("level", 4);
        dataList.add(data5);


        Map<String, Object> data6 = new HashMap<>();
        data6.put("type_id", "10011001100110011001");
        data6.put("level", 5);
        dataList.add(data6);

        return dataList;
    }

    private static List<TreeNode1> buildTree(List<Map<String, Object>> dataList) {
        Map<String, TreeNode1> nodeMap = new HashMap<>();

        // 第一遍遍历：将数据添加到节点映射表中
        for (Map<String, Object> data : dataList) {
            String typeId = (String) data.get("type_id");
            String parentId = getParentId(typeId);
            int level = (int) data.get("level");

            TreeNode1 node = new TreeNode1(typeId, parentId, level);
            nodeMap.put(typeId, node);
        }

        // 第二遍遍历：构建树结构
        List<TreeNode1> roots = new ArrayList<>();
        for (TreeNode1 node : nodeMap.values()) {
            String parentId = node.getParentId();
            if (parentId == null || parentId.isEmpty()) {
                roots.add(node);
            } else {
                TreeNode1 parent = nodeMap.get(parentId);
                if (parent != null) {
                    parent.addChild(node);
                }
            }
        }

        return roots;
    }

    private static String getParentId(String typeId) {
        // 根据type_id规则计算父节点的id
        if (typeId.length() <= 4) {
            return null; // 根节点没有父节点
        }
        return typeId.substring(0, typeId.length() - 4);
    }
}
