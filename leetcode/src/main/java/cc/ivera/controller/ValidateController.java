package cc.ivera.controller;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import cc.ivera.annotation.Age;
import cc.ivera.model.pojo.ValBean;
import cc.ivera.model.vo.AjaxResult;
import cc.ivera.util.BindingResultUtil;
import cc.ivera.util.JacksonUtil;
import cc.ivera.validator.AgeConstraintValidator;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping(value = "/validator")
public class ValidateController {

    @RequestMapping(value = "/test1", method = {RequestMethod.POST, RequestMethod.GET})
    public AjaxResult test1(@Valid @RequestBody ValBean b, BindingResult result) {
        if (result.hasErrors()) {
            Map<String, String> map = BindingResultUtil.getErrors(result);
            return AjaxResult.error("失败", map);
        } else {
            //继续业务逻辑    
            log.info("info {}", JacksonUtil.parseObjToJson(b));
        }
        return AjaxResult.success(b);
    }


    @RequestMapping(value = "/test2", method = {RequestMethod.POST, RequestMethod.GET})
    public AjaxResult test2(@Valid @RequestBody ValBean b) {
        //继续业务逻辑
        log.info("info {}", JacksonUtil.parseObjToJson(b));
        return AjaxResult.success(b);
    }

    @RequestMapping(value = "/test3/{name}", method = {RequestMethod.POST, RequestMethod.GET})
    public AjaxResult test3(@Valid @NotBlank(message = "username不能为空") @Length(max = 10, min = 2, message = "username最小2位，最大10位") @PathVariable String name) {
        log.info("info {}", name);
        return AjaxResult.success(name);
    }

    @RequestMapping(value = "/test4", method = {RequestMethod.POST, RequestMethod.GET})
    public AjaxResult test4(@Valid @NotBlank(message = "username不能为空") @Length(max = 6, min = 3, message = "username最小3位，最大6位") @RequestParam("name") String name) {
        log.info("info {}", name);
        return AjaxResult.success(name);
    }

    @RequestMapping(value = "/test5", method = {RequestMethod.POST, RequestMethod.GET})
    public AjaxResult test5(@Valid @Max(value = 20, message = "最大年龄20") @RequestParam("age") int age) {
        log.info("valV3_RequestParam_age info {}", age);
        return AjaxResult.success(age);
    }


    @RequestMapping(value = "/test6/{age}", method = {RequestMethod.POST, RequestMethod.GET})
    public AjaxResult test6(@Valid @Age(max = AgeConstraintValidator.MAXDEFAULT, min = AgeConstraintValidator.MINDEFAULT, message = AgeConstraintValidator.TIP_MESSAGE) @PathVariable int age) {
        log.info("{}", age);
        return AjaxResult.success(age);
    }


    @RequestMapping(method = {RequestMethod.POST, RequestMethod.GET})
    public AjaxResult v1(@Valid @Age(max = AgeConstraintValidator.MAXDEFAULT, min = AgeConstraintValidator.MINDEFAULT, message = AgeConstraintValidator.TIP_MESSAGE) @RequestParam("age") int age) {
        log.info("{}", age);
        return AjaxResult.success(age);
    }

}