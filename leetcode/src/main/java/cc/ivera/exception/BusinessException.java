package cc.ivera.exception;


import lombok.Data;
import cc.ivera.model.vo.ErrorResult;


@Data
public class BusinessException extends RuntimeException {

    private ErrorResult errorResult;

    public BusinessException(ErrorResult errorResult) {
        super(errorResult.getMessage());
        this.errorResult = errorResult;
    }
}
