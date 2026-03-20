package cc.ivera.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BindingResultUtil {
    private final static Logger log = LoggerFactory.getLogger(BindingResultUtil.class);

    public static Map<String, String> getErrors(BindingResult result) {
        Map<String, String> map = new HashMap<String, String>();
        List<FieldError> list = result.getFieldErrors();
        for (FieldError error : list) {
            log.info("error.getField(): {}", error.getField());
            log.info("error.getDefaultMessage(): {}", error.getDefaultMessage());
            map.put(error.getField(), error.getDefaultMessage());
        }
        return map;
    }
}
