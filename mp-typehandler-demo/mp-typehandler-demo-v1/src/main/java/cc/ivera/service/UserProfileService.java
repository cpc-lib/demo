package cc.ivera.service;

import cc.ivera.entity.TagItem;
import cc.ivera.entity.UserProfile;
import cc.ivera.mapper.UserProfileMapper;
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


    public Page<UserProfile> pageQuery(long current, long size, String name) {
        Page<UserProfile> page = new Page<>(current, size);

        LambdaQueryWrapper<UserProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(name), UserProfile::getName, name)
                .orderByDesc(UserProfile::getId);

        return userProfileMapper.selectPage(page, wrapper);
    }

    public Long addDemoUser() {
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

    public void updateDemoUser(Long id) {
        UserProfile user = userProfileMapper.selectById(id);
        if (user == null) {
            return;
        }

        user.setHobbies(Arrays.asList("电影", "游泳"));
        user.setTags(Arrays.asList(
                new TagItem(10L, "MySQL"),
                new TagItem(11L, "JSON")
        ));

        userProfileMapper.updateById(user);
    }

    public Long addCustomUser(String name, List<String> hobbies, List<TagItem> tags) {
        UserProfile user = new UserProfile();
        user.setName(name);
        user.setHobbies(hobbies);
        user.setTags(tags);
        userProfileMapper.insert(user);
        return user.getId();
    }
}
