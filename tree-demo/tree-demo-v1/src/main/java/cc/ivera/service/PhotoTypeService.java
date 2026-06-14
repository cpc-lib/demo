package cc.ivera.service;

import cc.ivera.dto.PhotoTypeSaveDTO;
import cc.ivera.entity.PhotoType;
import cc.ivera.exception.BusinessException;
import cc.ivera.mapper.PhotoTypeMapper;
import cc.ivera.util.TreeUtils;
import cc.ivera.vo.PhotoTypeVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PhotoTypeService {

    private static final String TREE_CACHE_KEY = "photo:type:tree";

    private final PhotoTypeMapper mapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private final ScheduledExecutorService cacheDeleteExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "photo-type-cache-delete");
        thread.setDaemon(true);
        return thread;
    });

    @Value("${photo.max-layer:10}")
    private int maxLayer;

    @Value("${photo.rule.enable-layer:3}")
    private Integer enableLayer;

    @Value("${photo.cache.ttl-minutes:30}")
    private long cacheTtlMinutes;

    @Value("${photo.cache.double-delete-delay-ms:100}")
    private long doubleDeleteDelayMs;

    @Autowired
    private ObjectMapper objectMapper;

    @Transactional(rollbackFor = Throwable.class)
    public void save(PhotoTypeSaveDTO dto) {

        PhotoType parent = "0".equals(dto.getParentId()) ? null : mapper.selectById(dto.getParentId());

        int layer = "0".equals(dto.getParentId()) ? 1 : Optional.ofNullable(parent).map(p -> p.getLayer() + 1).orElseThrow(() -> new BusinessException("父节点不存在"));

        if (layer > maxLayer) {
            throw new BusinessException("超过最大层级");
        }

        boolean hasRule = (dto.getNameRule() != null && !dto.getNameRule().trim().isEmpty()) && (dto.getNameRuleEn() != null && !dto.getNameRuleEn().trim().isEmpty());


        if (layer < 3) {
            dto.setNameRule(null);
            dto.setNameRuleEn(null);
        } else {
            if (hasRule) {
                throw new BusinessException("layer>=" + enableLayer + "需要设置命名规则");
            }
        }

        List<PhotoType> list = mapper.lockByParent(dto.getParentId());

        boolean exist = list.stream().anyMatch(x -> x.getName().equals(dto.getName()) && !Objects.equals(x.getId(), dto.getId()));

        if (exist) {
            throw new BusinessException("名称重复");
        }

        PhotoType entity = new PhotoType();
        BeanUtils.copyProperties(dto, entity);
        entity.setLayer(layer);
        entity.setAncestors(buildAncestors(dto.getParentId(), parent));

        if (dto.getId() == null) {
            entity.setId(UUID.randomUUID().toString());
            entity.setCreateTime(LocalDateTime.now());
        } else {
            entity.setUpdateTime(LocalDateTime.now());
        }

        list = list.stream().filter(x -> !Objects.equals(x.getId(), entity.getId())).sorted(Comparator.comparing(PhotoType::getSort)).collect(Collectors.toList());

        int target = (entity.getSort() == null || entity.getSort() > list.size()) ? list.size() + 1 : entity.getSort();

        if (target <= 0) {
            target = 1;
        }

        list.add(target - 1, entity);

        for (int i = 0; i < list.size(); i++) {
            list.get(i).setSort(i + 1);
        }

        mapper.batchUpdateSort(list);

        if (dto.getId() == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }

        deleteTreeCacheWithDoubleDelete();
    }

    public List<PhotoTypeVO> tree() {
        Object cached = redisTemplate.opsForValue().get(TREE_CACHE_KEY);
        if (cached instanceof List<?> cachedList) {
            @SuppressWarnings("unchecked") List<PhotoTypeVO> result = (List<PhotoTypeVO>) cachedList;
            return result;
        }

        LambdaQueryWrapper<PhotoType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PhotoType::getIsDeleted, 0);
        //using where;using filesort导致查询慢
        //.orderByAsc(PhotoType::getParentId).orderByAsc(PhotoType::getSort);

        List<PhotoType> all = mapper.selectList(wrapper);
        List<PhotoTypeVO> targetList = new ArrayList<>(all.size());
        all.stream().forEach(x -> {
            PhotoTypeVO photoTypeVO = new PhotoTypeVO();
            BeanUtils.copyProperties(x, photoTypeVO);
            targetList.add(photoTypeVO);
        });
        Map<String, List<PhotoTypeVO>> map = targetList.stream().collect(Collectors.groupingBy(PhotoTypeVO::getParentId));
        List<PhotoTypeVO> tree = buildTree("0", map);


        redisTemplate.opsForValue().set(TREE_CACHE_KEY, tree, cacheTtlMinutes, TimeUnit.MINUTES);
        return tree;
    }


    public List<PhotoTypeVO> tree(String name) {
        Object cached = redisTemplate.opsForValue().get(TREE_CACHE_KEY);
        if (cached instanceof List<?> cachedList) {
            @SuppressWarnings("unchecked") List<PhotoTypeVO> result = (List<PhotoTypeVO>) cachedList;
            if (!StringUtils.hasLength(name)) {
                return result;
            } else {


                List<PhotoTypeVO> tempTree = objectMapper.convertValue(result, new com.fasterxml.jackson.core.type.TypeReference<List<PhotoTypeVO>>() {
                });

                //List<PhotoTypeVO> photoTypeVOS = flattenTree(tempTree);


                List<PhotoTypeVO> photoTypeVOS = TreeUtils.flattenRecursive(tempTree, PhotoTypeVO::getChildren, node -> {
                    PhotoTypeVO item = new PhotoTypeVO();
                    item.setId(node.getId());
                    item.setName(node.getName());
                    item.setSort(node.getSort());
                    item.setParentId(node.getParentId());
                    item.setLayer(node.getLayer());
                    return item;
                });

                List<PhotoTypeVO> list = TreeUtils.flattenWithLevel(tempTree, PhotoTypeVO::getChildren, (node, level) -> {
                    PhotoTypeVO item = new PhotoTypeVO();
                    item.setId(node.getId());
                    item.setName(node.getName());
                    item.setSort(node.getSort());
                    item.setParentId(node.getParentId());

                    // 👇 如果你有 level 字段可以加
                    item.setLayer(level);

                    return item;
                });

                for (PhotoTypeVO photoTypeVO : list) {
                    System.out.println(photoTypeVO);
                }


                // 1. 找命中节点
                List<PhotoTypeVO> matched = photoTypeVOS.stream().filter(x -> x.getName() != null && x.getName().contains(name)).toList();
                if (matched.isEmpty()) {
                    return List.of();
                } else {
                    // 2. 收集所有父节点（递归向上）
                    Map<String, PhotoTypeVO> idMap = photoTypeVOS.stream().collect(Collectors.toMap(PhotoTypeVO::getId, x -> x));

                    Set<String> needIds = new HashSet<>();

                    for (PhotoTypeVO photoTypeVO : matched) {
                        collectParent(photoTypeVO, idMap, needIds);
                    }

                    // 3. 过滤出需要的节点
                    List<PhotoTypeVO> filtered = photoTypeVOS.stream().filter(x -> needIds.contains(x.getId())).toList();
                    Map<String, List<PhotoTypeVO>> group = group(filtered);
                    return buildTree("0", group);
                }

            }
        }

        LambdaQueryWrapper<PhotoType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PhotoType::getIsDeleted, 0);
        //using where;using filesort导致查询慢
        //.orderByAsc(PhotoType::getParentId).orderByAsc(PhotoType::getSort);

        List<PhotoType> all = mapper.selectList(wrapper);
        List<PhotoTypeVO> targetList = new ArrayList<>(all.size());
        all.stream().forEach(x -> {
            PhotoTypeVO photoTypeVO = new PhotoTypeVO();
            BeanUtils.copyProperties(x, photoTypeVO);
            targetList.add(photoTypeVO);
        });


        Map<String, List<PhotoTypeVO>> map = targetList.stream().collect(Collectors.groupingBy(PhotoTypeVO::getParentId));

        if (!StringUtils.hasLength(name)) {
            List<PhotoTypeVO> tree = buildTree("0", map);
            redisTemplate.opsForValue().set(TREE_CACHE_KEY, tree, cacheTtlMinutes, TimeUnit.MINUTES);
            return tree;
        } else {
            // 1. 找命中节点
            List<PhotoTypeVO> matched = targetList.stream().filter(x -> x.getName() != null && x.getName().contains(name)).toList();
            if (matched.isEmpty()) {
                return List.of();
            } else {
                // 2. 收集所有父节点（递归向上）
                Map<String, PhotoTypeVO> idMap = targetList.stream().collect(Collectors.toMap(PhotoTypeVO::getId, x -> x));
                Set<String> needIds = new HashSet<>();
                for (PhotoTypeVO photoTypeVO : matched) {
                    collectParent(photoTypeVO, idMap, needIds);
                }
                // 3. 过滤出需要的节点
                List<PhotoTypeVO> filtered = targetList.stream().filter(x -> needIds.contains(x.getId())).toList();
                Map<String, List<PhotoTypeVO>> group = group(filtered);
                List<PhotoTypeVO> returnData = buildTree("0", group);
                redisTemplate.opsForValue().set(TREE_CACHE_KEY, returnData, cacheTtlMinutes, TimeUnit.MINUTES);
                return returnData;
            }
        }


    }


    private String buildAncestors(String parentId, PhotoType parent) {
        if (parentId == null || "0".equals(parentId)) {
            return "0";
        }
        if (parent == null) {
            throw new BusinessException("父节点不存在");
        }
        return parent.getAncestors() + "," + parent.getId();
    }

    private void deleteTreeCacheWithDoubleDelete() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    redisTemplate.delete(TREE_CACHE_KEY);
                    cacheDeleteExecutor.schedule(() -> redisTemplate.delete(TREE_CACHE_KEY), doubleDeleteDelayMs, TimeUnit.MILLISECONDS);
                }
            });
            return;
        }

        redisTemplate.delete(TREE_CACHE_KEY);
        cacheDeleteExecutor.schedule(() -> redisTemplate.delete(TREE_CACHE_KEY), doubleDeleteDelayMs, TimeUnit.MILLISECONDS);
    }


    private Map<String, List<PhotoTypeVO>> group(List<PhotoTypeVO> list) {
        return list.stream().collect(Collectors.groupingBy(PhotoTypeVO::getParentId));
    }


    private List<PhotoTypeVO> buildTree(String pid, Map<String, List<PhotoTypeVO>> map) {
        List<PhotoTypeVO> children = map.get(pid);
        if (children == null) {
            return List.of();
        }

        return children.stream().map(e -> {
            PhotoTypeVO vo = new PhotoTypeVO();
            BeanUtils.copyProperties(e, vo);
            vo.setChildren(buildTree(e.getId(), map));
            return vo;
        }).toList();
    }


//    public List<PhotoTypeVO> flattenTree(List<PhotoTypeVO> tree) {
//        List<PhotoTypeVO> result = new ArrayList<>();
//        if (tree == null || tree.isEmpty()) {
//            return result;
//        }
//
//        for (PhotoTypeVO node : tree) {
//            dfs(node, result);
//        }
//        return result;
//    }
//
//    private void dfs(PhotoTypeVO node, List<PhotoTypeVO> result) {
//        if (node == null) {
//            return;
//        }
//
//        PhotoTypeVO item = new PhotoTypeVO();
//        item.setId(node.getId());
//        item.setName(node.getName());
//        item.setSort(node.getSort());
//        item.setParentId(node.getParentId());
//        item.setChildren(null); // 拍平后不需要 children
//
//        result.add(item);
//
//        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
//            for (PhotoTypeVO child : node.getChildren()) {
//                dfs(child, result);
//            }
//        }
//    }


    private void collectParent(PhotoTypeVO node, Map<String, PhotoTypeVO> map, Set<String> set) {

        if (node == null || set.contains(node.getId())) return;

        set.add(node.getId());

        if (!"0".equals(node.getParentId())) {
            collectParent(map.get(node.getParentId()), map, set);
        }
    }
}
