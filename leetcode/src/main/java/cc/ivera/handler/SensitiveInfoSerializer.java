package cc.ivera.handler;

//springboot模式使用jackson

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import cc.ivera.annotation.Desensitization;
import cc.ivera.enums.DesensitizationType;

import java.io.IOException;

/**
 * 数据脱敏序列化器
 */
public class SensitiveInfoSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private boolean useMasking = false;
    private DesensitizationType type;
    private int prefixLen;
    private int suffixLen;
    private String maskingChar;


    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        if (property != null) {
            Desensitization desensitization = property.getAnnotation(Desensitization.class);
            if (desensitization != null) {
                this.type = desensitization.type();
                this.prefixLen = desensitization.prefixLen();
                this.suffixLen = desensitization.suffixLen();
                this.maskingChar = desensitization.maskingChar();
                useMasking = true;
            }
        }
        return this;
    }

    @Override
    public void serialize(String value, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if (useMasking && value != null) {
            switch (type) {
                case MOBILE_PHONE:
                    jsonGenerator.writeString(DesensitizedUtil.mobilePhone(value));
                    break;
                case ID_CARD:
                    jsonGenerator.writeString(DesensitizedUtil.idCardNum(value, prefixLen, suffixLen));
                    break;
                case CUSTOMIZE_RULE:
//                    gen.writeString(StrUtil.replace(value, prefixLen, suffixLen, maskingChar));
                    jsonGenerator.writeString(StrUtil.hide(value, prefixLen, suffixLen));
                    break;
                case CHINESE_NAME:
                    jsonGenerator.writeString(DesensitizedUtil.chineseName(value));
                    break;
                case DEFAULT:
                    jsonGenerator.writeString(value);
                default:
                    jsonGenerator.writeString(value);
            }
        } else {
            jsonGenerator.writeObject(value);
        }
    }
}

