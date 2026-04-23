package cc.ivera.util;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

public interface Treeable<T> {

    /***
     * 顶级目录的父目录id为null
     */
    public Object rootKey = null;

    /**
     * 获取存入map key
     *
     * @return
     */
    @JsonIgnore
    Object getMapKey();

    /**
     * 获取孩子节点的key值
     *
     * @return
     */
    @JsonIgnore
    Object getChildrenKey();

    /**
     * 根节点key值
     *
     * @return
     */
    @JsonIgnore
    Object getRootKey();

    /**
     * 子节点赋值
     *
     * @param children
     */
    void setChildren(List<T> children);


}