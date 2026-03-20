package cc.ivera.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//约定n为编码为一个界别的树形数据构建
public class TreeBuilder3 {

    public static List<TreeNode3> buildTree(Map<String, TreeNode3> nodeMap) {
        // 第二遍遍历：构建树结构
        List<TreeNode3> roots = new ArrayList<>();
        for (TreeNode3 node : nodeMap.values()) {
            String parentId = node.getParentId();
            if (parentId == null || parentId.isEmpty()) {
                roots.add(node);
            } else {
                TreeNode3 parent = nodeMap.get(parentId);
                if (parent != null) {
                    parent.addChild(node);
                }
            }
        }
        return roots;
    }

    public static String getParentId(String typeId) {
        //根据type_id规则计算父节点的id
        if (typeId.length() <= 4) {
            // 根节点没有父节点
            return "";
        }
        return typeId.substring(0, typeId.length() - 4);
    }
}
