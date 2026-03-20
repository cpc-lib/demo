package cc.ivera.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.util.ObjectUtils;
import cc.ivera.model.pojo.Place;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface TreeUtil<T extends Treeable> {

    /**
     * 懒加载实现方式
     *
     * @param list
     * @param rootKey
     * @param level
     * @param deepth
     * @return
     */
    static List toTree(List list, Object rootKey, int level, int deepth) {
        if (ObjectUtils.isEmpty(list)) {
            return new ArrayList();
        }
        Map<Object, List> map = new HashMap<Object, List>();
        for (Object o : list) {
            Treeable t = (Treeable) o;
            Object key = t.getMapKey();
            if (map.containsKey(key)) {
                map.get(key).add(o);
            } else {
                List mapValue = new ArrayList();
                mapValue.add(o);
                map.put(key, mapValue);
            }
            // 获取根节点key值
            if (rootKey == null) {
                rootKey = t.getRootKey();
            }
        }
        List tree = map.get(rootKey);
        recursionToTree(tree, map, level, deepth);
        return tree;
    }

    static void recursionToTree(List list, Map<Object, List> map, int level, int deepth) {
        if (ObjectUtils.isEmpty(list)) {
            return;
        }
        for (Object o : list) {
            Treeable t = (Treeable) o;
            Object key = t.getChildrenKey();
            if (map.containsKey(key)) {
                List children = map.get(key);
                t.setChildren(children);
                if ((level++) < deepth) {
                    recursionToTree(children, map, level, deepth);
                } else {
                    return;
                }
            }
        }
    }

    static List<Place> recursive(JSONArray children, JSONObject jsonObject, List list, Class<? extends Treeable> clazz) {
        if (children.size() == 0) {
            return list;
        }
        for (int i = 0; i < children.size(); i++) {
            jsonObject = (JSONObject) children.get(i);
            Treeable treeable = JSONObject.parseObject(jsonObject.toJSONString(), clazz);
            treeable.setChildren(null);
            list.add(treeable);
            JSONArray childrenTemp = jsonObject.getJSONArray("children");
            recursive(childrenTemp, jsonObject, list, clazz);
        }
        return list;
    }

}
