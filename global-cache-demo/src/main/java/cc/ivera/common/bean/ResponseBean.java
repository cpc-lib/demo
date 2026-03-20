package cc.ivera.common.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseBean {
    private Integer code;
    private String msg;
    private Object data;

    private final static ResponseBean SUCCSSS = new ResponseBean(200,"SUCCESS");
    private final static ResponseBean FAIL = new ResponseBean(500,"FAIL");

    public ResponseBean(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public static ResponseBean ok(){
        return SUCCSSS;
    }

    public static ResponseBean ok(Object data){
        ResponseBean responseBean = new ResponseBean(200,"success",data);
        return responseBean;
    }

    public static ResponseBean fail(){
        return FAIL;
    }

}
