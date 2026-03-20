package cc.ivera.test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TreeNode1 implements Serializable {
    private String id;
    private String parentId;
    private int level;
    private List<TreeNode1> children;

    public TreeNode1(String id, String parentId, int level) {
        this.id = id;
        this.parentId = parentId;
        this.level = level;
        this.children = new ArrayList<>();
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public String getParentId() {
        return parentId;
    }

    public int getLevel() {
        return level;
    }

    public List<TreeNode1> getChildren() {
        return children;
    }

    public void addChild(TreeNode1 child) {
        children.add(child);
    }
}