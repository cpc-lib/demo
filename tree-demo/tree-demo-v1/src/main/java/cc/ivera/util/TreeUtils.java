package cc.ivera.util;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class TreeUtils {

    /**
     * 通用递归拍平树
     *
     * @param tree           树
     * @param childrenGetter 获取子节点
     * @param mapper         转换函数
     */
    public static <T, R> List<R> flattenRecursive(List<T> tree, Function<T, List<T>> childrenGetter, Function<T, R> mapper) {

        List<R> result = new ArrayList<>();
        if (tree == null || tree.isEmpty()) {
            return result;
        }

        for (T node : tree) {
            dfs(node, childrenGetter, mapper, result);
        }

        return result;
    }

    private static <T, R> void dfs(T node, Function<T, List<T>> childrenGetter, Function<T, R> mapper, List<R> result) {

        // 处理当前节点（前序遍历）
        result.add(mapper.apply(node));

        List<T> children = childrenGetter.apply(node);
        if (children == null || children.isEmpty()) {
            return;
        }

        for (T child : children) {
            dfs(child, childrenGetter, mapper, result);
        }
    }


    public static <T, R> List<R> flattenWithLevel(List<T> tree, Function<T, List<T>> childrenGetter, BiFunction<T, Integer, R> mapper) {

        List<R> result = new ArrayList<>();
        if (tree == null || tree.isEmpty()) {
            return result;
        }

        for (T node : tree) {
            dfs(node, childrenGetter, mapper, result, 1);
        }

        return result;
    }

    private static <T, R> void dfs(T node, Function<T, List<T>> childrenGetter, BiFunction<T, Integer, R> mapper, List<R> result, int level) {

        result.add(mapper.apply(node, level));

        List<T> children = childrenGetter.apply(node);
        if (children == null || children.isEmpty()) {
            return;
        }

        for (T child : children) {
            dfs(child, childrenGetter, mapper, result, level + 1);
        }
    }
}