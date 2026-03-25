
package cc.ivera.service;

import cc.ivera.dto.PhotoTypeSaveDTO;
import cc.ivera.entity.PhotoType;
import cc.ivera.exception.BusinessException;
import cc.ivera.mapper.PhotoTypeMapper;
import cc.ivera.vo.PhotoTypeVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PhotoTypeService {

    private final PhotoTypeMapper mapper;

    @Value("${photo.max-layer:10}")
    private int maxLayer;

    @Value("${photo.rule.enable-layer:3}")
    private Integer enableLayer;

    @Transactional
    public void save(PhotoTypeSaveDTO dto) {

        int layer = "0".equals(dto.getParentId()) ? 1 :
                Optional.ofNullable(mapper.selectById(dto.getParentId()))
                        .map(p -> p.getLayer() + 1)
                        .orElseThrow(() -> new BusinessException("父节点不存在"));

        if (layer > maxLayer) {
            throw new BusinessException("超过最大层级");
        }

        boolean hasRule = (dto.getNameRule() != null && !dto.getNameRule().trim().isEmpty())
                && (dto.getNameRuleEn() != null && !dto.getNameRuleEn().trim().isEmpty());

        if (!(layer >= 3 && hasRule)) {
            throw new BusinessException("layer>=" + enableLayer + "才允许命名规则");
        }

        List<PhotoType> list = mapper.lockByParent(dto.getParentId());

        boolean exist = list.stream()
                .anyMatch(x -> x.getName().equals(dto.getName())
                        && !Objects.equals(x.getId(), dto.getId()));

        if (exist) {
            throw new BusinessException("名称重复");
        }

        PhotoType entity = new PhotoType();
        BeanUtils.copyProperties(dto, entity);
        entity.setLayer(layer);

        if (dto.getId() == null) {
            entity.setId(UUID.randomUUID().toString());
            entity.setCreateTime(LocalDateTime.now());
        } else {
            entity.setUpdateTime(LocalDateTime.now());
        }

        list = list.stream()
                .filter(x -> !Objects.equals(x.getId(), entity.getId()))
                .sorted(Comparator.comparing(PhotoType::getSort))
                .collect(Collectors.toList());

        int target = (entity.getSort() == null || entity.getSort() > list.size())
                ? list.size() + 1 : entity.getSort();

        list.add(target - 1, entity);

        for (int i = 0; i < list.size(); i++) {
            list.get(i).setSort(i + 1);
            //mapper.updateById(list.get(i));
        }

        // 一次性批量更新
        mapper.batchUpdateSort(list);

        if (dto.getId() == null) mapper.insert(entity);
        else mapper.updateById(entity);
    }

    public List<PhotoTypeVO> tree() {
        List<PhotoType> all = mapper.selectList(null);

        Map<String, List<PhotoType>> map =
                all.stream().collect(Collectors.groupingBy(PhotoType::getParentId));

        return build("0", map);
    }

    private List<PhotoTypeVO> build(String pid, Map<String, List<PhotoType>> map) {
        List<PhotoType> children = map.get(pid);
        if (children == null) return List.of();

        return children.stream().map(e -> {
            PhotoTypeVO vo = new PhotoTypeVO();
            BeanUtils.copyProperties(e, vo);
            vo.setChildren(build(e.getId(), map));
            return vo;
        }).toList();
    }

    public List<PhotoTypeVO> tree(String name) {

        LambdaQueryWrapper<PhotoType> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(PhotoType::getIsDeleted, 0)
                .orderByAsc(PhotoType::getParentId)
                .orderByAsc(PhotoType::getSort);

        List<PhotoType> all = mapper.selectList(wrapper);

        // 不搜索 → 直接全量树
        if (name == null || name.isBlank()) {
            return buildTree("0", group(all));
        }

        // 1. 找命中节点
        List<PhotoType> matched = all.stream()
                .filter(x -> x.getName() != null && x.getName().contains(name))
                .toList();

        if (matched.isEmpty()) {
            return List.of();
        }

        // 2. 收集所有父节点（递归向上）
        Map<String, PhotoType> idMap =
                all.stream().collect(Collectors.toMap(PhotoType::getId, x -> x));

        Set<String> needIds = new HashSet<>();

        for (PhotoType m : matched) {
            collectParent(m, idMap, needIds);
        }

        // 3. 过滤出需要的节点
        List<PhotoType> filtered = all.stream()
                .filter(x -> needIds.contains(x.getId()))
                .toList();

        return buildTree("0", group(filtered));
    }


    private void collectParent(PhotoType node,
                               Map<String, PhotoType> map,
                               Set<String> set) {

        if (node == null || set.contains(node.getId())) return;

        set.add(node.getId());

        if (!"0".equals(node.getParentId())) {
            collectParent(map.get(node.getParentId()), map, set);
        }
    }


    private Map<String, List<PhotoType>> group(List<PhotoType> list) {
        return list.stream().collect(Collectors.groupingBy(PhotoType::getParentId));
    }

    private List<PhotoTypeVO> buildTree(String parentId,
                                        Map<String, List<PhotoType>> map) {

        List<PhotoType> children = map.get(parentId);
        if (children == null) return List.of();

        return children.stream().map(e -> {
            PhotoTypeVO vo = new PhotoTypeVO();
            BeanUtils.copyProperties(e, vo);
            vo.setChildren(buildTree(e.getId(), map));
            return vo;
        }).toList();
    }
}
