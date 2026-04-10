package cc.ivera.audit.provider;

import cc.ivera.entity.UserProfile;
import cc.ivera.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserProfileSnapshotProvider implements ChangeLogSnapshotProvider {

    private final UserProfileMapper userProfileMapper;

    @Override
    public boolean supports(Class<?> entityClass) {
        return UserProfile.class.equals(entityClass);
    }

    @Override
    public Object loadById(Object id) {
        if (id == null) {
            return null;
        }
        if (id instanceof Number number) {
            return userProfileMapper.selectById(number.longValue());
        }
        return userProfileMapper.selectById(Long.parseLong(String.valueOf(id)));
    }
}
