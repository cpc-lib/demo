package cc.ivera.serrvice;

import cc.ivera.common.exception.CustomException;
import cc.ivera.common.utils.CurrentUserUtils;
import cc.ivera.common.utils.JwtTokenUtils;
import cc.ivera.entity.JwtUser;
import cc.ivera.entity.LoginRequest;
import cc.ivera.entity.Token;
import cc.ivera.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author shuang.kou
 **/
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AuthService {
    private final UserService userService;
//    private final StringRedisTemplate stringRedisTemplate;
    private final CurrentUserUtils currentUserUtils;

    public Token createToken(LoginRequest loginRequest) {
        User user = userService.find(loginRequest.getUsername());
        //$2a$10$I.1v3mSLdhrCHF6ozNHuq.sI42mMaNE6KMKq3.jZGF69M5sFzzlBm
        if (!userService.check(loginRequest.getPassword(), user.getPassword())) {
            throw new CustomException("The user name or password is not correct.");
        }
        JwtUser jwtUser = new JwtUser(user);
        if (!jwtUser.isEnabled()) {
            throw new CustomException("User is forbidden to login");
        }
//        stringRedisTemplate.opsForValue().set(user.getId().toString(), token);

        return JwtTokenUtils.createToken(user.getUsername()
                , user.getId().toString()
                , Objects.isNull(user.getRememberMe()) || user.getRememberMe());
    }
//    public String createAccessToken(User loginRequest) {
//        User user = userService.find(loginRequest.getUsername());
//        if (!userService.check(loginRequest.getPassword(), user.getPassword())) {
//            throw new BadCredentialsException("The user name or password is not correct.");
//        }
//        JwtUser jwtUser = new JwtUser(user);
//        if (!jwtUser.isEnabled()) {
//            throw new BadCredentialsException("User is forbidden to login");
//        }
//        String token = JwtTokenUtils.createAccessToken(user.getUsername(), user.getId().toString());
////        stringRedisTemplate.opsForValue().set(user.getId().toString(), token);
//        return token;
//    }
    /**
     * 双token不能实现登录绝对安全，所以要保证登录的可见性，在缓存总存入登录信息，在系统中可查看登录列表并可下线登录
     */
//    public void removeToken() {
//        stringRedisTemplate.delete(currentUserUtils.getCurrentUser().getId().toString());
//    }
}
