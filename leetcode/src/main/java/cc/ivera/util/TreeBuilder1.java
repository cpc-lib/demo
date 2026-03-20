package cc.ivera.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TreeBuilder1 {

    public static List<TreeNode1> buildTree(List<Map<String, Object>> dataList) {
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
        //根据type_id规则计算父节点的id
        if (typeId.length() <= 4) {
            // 根节点没有父节点
            return null;
        }
        return typeId.substring(0, typeId.length() - 4);
    }
}
