package cc.ivera.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import cc.ivera.common.constants.SecurityConstants;
import cc.ivera.common.utils.AjaxResult;
import cc.ivera.common.utils.JwtTokenUtils;
import cc.ivera.entity.LoginRequest;
import cc.ivera.entity.Token;
import cc.ivera.serrvice.AuthService;

/**
 * 认证控制器
 *
 * @author LiYunFei
 * @date 2023/6/20 21:50
 */
@RestController
@RequestMapping("/auth")
@Api("认证")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    @ApiOperation("登录")
    public AjaxResult login(@RequestBody @Validated LoginRequest loginRequest) {
        Token token = authService.createToken(loginRequest);
        return AjaxResult.success(token);
    }

    @PostMapping("/refresh")
    @ApiOperation(value = "刷新token", notes = "当登录超过过了一段时间，发生401时，说明accessToken过期，需要调用本接口刷新两个token")
    public AjaxResult refresh(@RequestHeader(SecurityConstants.TOKEN_HEADER) String refreshToken) {
        return AjaxResult.success(JwtTokenUtils.refreshToken(refreshToken.replace(SecurityConstants.TOKEN_PREFIX, "")));
    }
}
