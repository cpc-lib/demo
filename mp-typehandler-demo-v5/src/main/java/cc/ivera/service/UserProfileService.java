package cc.ivera.service;

import cc.ivera.audit.annotation.ChangeLog;
import cc.ivera.audit.enums.ChangeLogMode;
import cc.ivera.dto.ChangeLogResponse;
import cc.ivera.dto.UpdateUserRequest;
import cc.ivera.entity.TagItem;
import cc.ivera.entity.UserProfile;
import cc.ivera.mapper.UserProfileMapper;
import cc.ivera.service.audit.ChangeLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileMapper userProfileMapper;
    private final ChangeLogService changeLogService;

    public Page<UserProfile> pageQuery(long current, long size, String name) {
        Page<UserProfile> page = new Page<>(current, size);

        LambdaQueryWrapper<UserProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(name), UserProfile::getName, name)
                .orderByDesc(UserProfile::getId);

        return userProfileMapper.selectPage(page, wrapper);
    }

    @ChangeLog(
            bizType = "user_profile",
            entityClass = UserProfile.class,
            bizId = "#result",
            operator = "#operator != null && !#operator.isBlank() ? #operator : 'system'",
            requestUri = "#requestUri",
            operationType = "CREATE",
            mode = ChangeLogMode.ANNOTATION
    )
    public Long addDemoUser(String operator, String requestUri) {
        UserProfile user = new UserProfile();
        user.setName("张三");
        user.setHobbies(Arrays.asList("篮球", "游戏", "旅行"));
        user.setTags(Arrays.asList(
                new TagItem(1L, "Java"),
                new TagItem(2L, "SpringBoot"),
                new TagItem(3L, "MyBatis-Plus")
        ));

        userProfileMapper.insert(user);
        return user.getId();
    }

    public UserProfile getById(Long id) {
        return userProfileMapper.selectById(id);
    }

    @ChangeLog(
            bizType = "user_profile",
            entityClass = UserProfile.class,
            bizId = "#result",
            operator = "#operator != null && !#operator.isBlank() ? #operator : 'system'",
            requestUri = "#requestUri",
            operationType = "CREATE",
            mode = ChangeLogMode.ANNOTATION
    )
    public Long addCustomUser(String name, List<String> hobbies, List<TagItem> tags, String operator, String requestUri) {
        UserProfile user = new UserProfile();
        user.setName(name);
        user.setHobbies(hobbies);
        user.setTags(tags);
        userProfileMapper.insert(user);
        return user.getId();
    }

    @ChangeLog(
            bizType = "user_profile",
            entityClass = UserProfile.class,
            bizId = "#id",
            operator = "#operator != null && !#operator.isBlank() ? #operator : 'system'",
            requestUri = "#requestUri",
            mode = ChangeLogMode.ANNOTATION
    )
    public boolean updateWithAnnotation(Long id, UpdateUserRequest request, String operator, String requestUri) {
        UserProfile current = userProfileMapper.selectById(id);
        if (current == null) {
            return false;
        }
        applyUpdate(current, request);
        return userProfileMapper.updateById(current) > 0;
    }

    @ChangeLog(
            bizType = "user_profile",
            entityClass = UserProfile.class,
            bizId = "#id",
            operator = "#operator != null && !#operator.isBlank() ? #operator : 'system'",
            requestUri = "#requestUri",
            mode = ChangeLogMode.HARDCODED,
            hardcodedFields = {"name:姓名", "hobbies:爱好", "tags:标签"}
    )
    public boolean updateWithHardcoded(Long id, UpdateUserRequest request, String operator, String requestUri) {
        UserProfile current = userProfileMapper.selectById(id);
        if (current == null) {
            return false;
        }
        applyUpdate(current, request);
        return userProfileMapper.updateById(current) > 0;
    }

    @ChangeLog(
            bizType = "user_profile",
            entityClass = UserProfile.class,
            bizId = "#id",
            operator = "#operator != null && !#operator.isBlank() ? #operator : 'system'",
            requestUri = "#requestUri",
            operationType = "DELETE",
            mode = ChangeLogMode.ANNOTATION
    )
    public boolean deleteById(Long id, String operator, String requestUri) {
        return userProfileMapper.deleteById(id) > 0;
    }

    public List<ChangeLogResponse> listChangeLogs(Long id) {
        return changeLogService.listByBiz("user_profile", String.valueOf(id));
    }

    private void applyUpdate(UserProfile current, UpdateUserRequest request) {
        if (request.getName() != null) {
            current.setName(request.getName());
        }
        if (request.getHobbies() != null) {
            current.setHobbies(request.getHobbies());
        }
        if (request.getTags() != null) {
            current.setTags(request.getTags());
        }
    }
}
