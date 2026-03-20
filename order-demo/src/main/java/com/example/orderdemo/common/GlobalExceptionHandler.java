package com.example.orderdemo.common;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public Result<Void> handleApi(ApiException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValidation(Exception e) {
        String msg = "参数校验失败";
        if (e instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException ex = (MethodArgumentNotValidException) e;
            if (ex.getBindingResult().getFieldError() != null) {
                msg = ex.getBindingResult().getFieldError().getField() + " " +
                        ex.getBindingResult().getFieldError().getDefaultMessage();
            }
        } else if (e instanceof BindException) {
            BindException ex = (BindException) e;
            if (ex.getBindingResult().getFieldError() != null) {
                msg = ex.getBindingResult().getFieldError().getField() + " " +
                        ex.getBindingResult().getFieldError().getDefaultMessage();
            }
        }
        return Result.fail(400, msg);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException e) {
        return Result.fail(400, "请求体格式错误或缺少字段");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        return Result.fail(500, "服务器内部错误: " + e.getMessage());
    }
}
