package cc.ivera.util;

import cc.ivera.model.vo.TreeBean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TreeNode3 implements Serializable {
    private String id;
    private String parentId;
    private int level;
    private List<TreeNode3> children;

    public TreeNode3(String id, String parentId, int level) {
        this.id = id;
        this.parentId = parentId;
        this.level = level;
        this.children = new ArrayList<>();
    }

    // Getters and Setters

    public static void printParentNodes(TreeNode3 node, Map<String, TreeNode3> nodeMap, Map<String, TreeBean> treeBeanMap) {
        if (node.isParentNode()) {
            String id = node.getId();
            String pid = node.getParentId();
            TreeBean treeBean = treeBeanMap.get(id);
            treeBean.setPId(pid);
            treeBean.setParent(true);
        } else {
            String id = node.getId();
            String pid = node.getParentId();
            TreeBean treeBean = treeBeanMap.get(id);
            treeBean.setPId(pid);
            treeBean.setParent(false);
        }
        if (node.getChildren() != null) {
            for (TreeNode3 child : node.getChildren()) {
                printParentNodes(child, nodeMap, treeBeanMap); // 递归处理子节点
            }
        }
    }

    public String getId() {
        return id;
    }

    public String getParentId() {
        return parentId;
    }

    public int getLevel() {
        return level;
    }

    public List<TreeNode3> getChildren() {
        return children;
    }

    public void addChild(TreeNode3 child) {
        children.add(child);
    }

    @Override
    public String toString() {
        return "TreeNode{" + "id='" + id + '\'' + ", parentId='" + parentId + '\'' + ", level=" + level + ", children=" + children + '}';
    }

    // 判断节点是否是父节点的方法
    public boolean isParentNode() {
        return children != null && !children.isEmpty();
    }
}
