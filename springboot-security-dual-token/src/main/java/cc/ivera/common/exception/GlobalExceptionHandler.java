package cc.ivera.common.exception;

import cc.ivera.common.utils.AjaxResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;


/**
 * @author shuang.kou
 */
@ControllerAdvice
@ResponseBody
@Slf4j
public class GlobalExceptionHandler {
    /**
     * @Author: LiYunFei
     * @Date: 2022-04-30 23:01
     * @Description: 自定义异常处理方法
     */
    @ExceptionHandler(CustomException.class)
    public AjaxResult exceptionHandler(CustomException ex){
        return AjaxResult.error(ex.getMessage());
    }

    /**
     * @Author: LiYunFei
     * @Date: 2022-04-30 23:01
     * @Description: 异常处理方法
     */
    @ExceptionHandler(RuntimeException.class)
    public AjaxResult exceptionHandler(RuntimeException ex){
        ex.printStackTrace();
        return AjaxResult.error("系统异常！");
    }
}
