package cc.ivera.excel;

import cc.ivera.entity.TagItem;
import cc.ivera.entity.UserProfile;
import com.alibaba.fastjson2.JSON;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public enum UserProfileExportColumn {

    ID("id", "ID", user -> user.getId()),
    NAME("name", "姓名", UserProfile::getName),
    HOBBIES("hobbies", "爱好", user -> join(user.getHobbies())),
    TAGS("tags", "标签", user -> {
        List<TagItem> tags = user.getTags();
        if (tags == null || tags.isEmpty()) {
            return "[]";
        }
        return JSON.toJSONString(tags);
    });

    private static final Map<String, UserProfileExportColumn> CACHE = Arrays.stream(values())
            .collect(Collectors.toMap(item -> item.code.toLowerCase(Locale.ROOT), Function.identity()));

    private final String code;
    private final String head;
    private final Function<UserProfile, Object> extractor;

    UserProfileExportColumn(String code, String head, Function<UserProfile, Object> extractor) {
        this.code = code;
        this.head = head;
        this.extractor = extractor;
    }

    public static UserProfileExportColumn fromCode(String code) {
        return Optional.ofNullable(CACHE.get(code.toLowerCase(Locale.ROOT)))
                .orElseThrow(() -> new IllegalArgumentException("不支持的导出字段: " + code));
    }

    private static String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join(",", values);
    }
}
