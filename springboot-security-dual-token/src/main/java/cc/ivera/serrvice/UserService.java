package cc.ivera.serrvice;

import cc.ivera.common.exception.CustomException;
import cc.ivera.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserService {
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    /**
     * 根据用户名查找用户
     * @param currentUserName
     * @return
     */
    public User find(String currentUserName) {
//        return userRepository.findByUserName(userName).orElseThrow(() -> new UserNameNotFoundException(ImmutableMap.of(USERNAME, userName)));;
        //模仿数据库查找的该用户
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setEnabled(true);
        //$2a$10$I.1v3mSLdhrCHF6ozNHuq.sI42mMaNE6KMKq3.jZGF69M5sFzzlBm
        user.setPassword("$2a$10$I.1v3mSLdhrCHF6ozNHuq.sI42mMaNE6KMKq3.jZGF69M5sFzzlBm");
        return user;
    }
    public boolean check(String currentPassword, String password) {
        return this.bCryptPasswordEncoder.matches(currentPassword, password);
    }

    /**
     * 查询用户是否存在
     * @param userName
     */
    private void ensureUserNameNotExist(String userName) {
        //模拟数据库
//        boolean exist = userRepository.findByUserName(userName).isPresent();
        boolean exist = false;
        if (exist) {
            throw new CustomException("用户不存在");
        }
    }
}
