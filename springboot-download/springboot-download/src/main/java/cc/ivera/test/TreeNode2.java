package cc.ivera.test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TreeNode2 implements Serializable {
    private String id;
    private String parentId;
    private int level;
    private List<TreeNode2> children;

    public TreeNode2(String id, String parentId, int level) {
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

    public List<TreeNode2> getChildren() {
        return children;
    }

    public void addChild(TreeNode2 child) {
        children.add(child);
    }

    public void setLevel(int level) {
        this.level = level;
    }
}