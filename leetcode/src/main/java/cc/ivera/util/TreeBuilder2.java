package cc.ivera.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TreeBuilder2 {


    public static List<TreeNode2> buildTree(List<Map<String, Object>> dataList) {
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
