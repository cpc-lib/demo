package com.example.vocab.service.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.vocab.dto.auth.AuthRequest;
import com.example.vocab.dto.auth.AuthResponse;
import com.example.vocab.dto.auth.ChangePasswordRequest;
import com.example.vocab.dto.auth.RegisterRequest;
import com.example.vocab.entity.auth.AppUser;
import com.example.vocab.mapper.auth.AppUserMapper;
import com.example.vocab.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AppUserMapper appUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }
        AppUser existed = appUserMapper.selectOne(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getUsername, request.getUsername().trim())
                .last("LIMIT 1"));
        if (existed != null) throw new IllegalArgumentException("用户名已存在");
        AppUser existedCode = appUserMapper.selectOne(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getUserCode, request.getUserCode().trim())
                .last("LIMIT 1"));
        if (existedCode != null) throw new IllegalArgumentException("用户编号已存在");
        AppUser user = new AppUser();
        user.setUsername(request.getUsername().trim());
        user.setUserCode(request.getUserCode().trim());
        user.setNickname(request.getUsername().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(1);
        appUserMapper.insert(user);
        return toResponse(user);
    }

    public AuthResponse login(AuthRequest request) {
        AppUser user = appUserMapper.selectOne(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getUsername, request.getUsername().trim())
                .last("LIMIT 1"));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (Integer.valueOf(0).equals(user.getStatus())) throw new IllegalArgumentException("账户已被禁用");
        return toResponse(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new IllegalArgumentException("两次输入的新密码不一致");
        }
        AppUser user = appUserMapper.selectById(userId);
        if (user == null) throw new IllegalArgumentException("用户不存在");
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("旧密码错误");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        appUserMapper.updateById(user);
    }

    private AuthResponse toResponse(AppUser user) {
        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .token(jwtService.issue(user.getId(), user.getUsername()))
                .build();
    }
}
