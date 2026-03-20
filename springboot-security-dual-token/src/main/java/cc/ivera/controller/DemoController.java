package cc.ivera.controller;

import cc.ivera.common.utils.AjaxResult;
import cc.ivera.common.utils.CurrentUserUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试控制器
 * @author LiYunFei
 * @date 2023/6/20 21:50
 */
@RestController
@RequestMapping("/api")
@Api("测试")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DemoController {
    private final CurrentUserUtils currentUserUtils;
    @PostMapping("/hello")
    @ApiOperation(value = "业务测试",notes = "当未携带有效token或者过期，需要刷新token")
    public AjaxResult hello(){
        return AjaxResult.success("你好"+currentUserUtils.getCurrentUser().getUsername());
    }
}
