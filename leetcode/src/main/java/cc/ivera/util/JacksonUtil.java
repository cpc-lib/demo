package cc.ivera.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class JacksonUtil {

    private static final Logger log = LoggerFactory.getLogger(JacksonUtil.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Convert Java object to JSON string.
     *
     * @param object Object to be converted to JSON string.
     * @return JSON string or null.
     */
    public static String parseObjToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    /**
     * Convert JSON string to corresponding Java object.
     *
     * @param json JSON string.
     * @param c    Type of the corresponding object.
     * @param <T>  Type of the object.
     * @return Corresponding Java object or null.
     */
    public static <T> T parseJsonToObj(String json, Class<T> c) {
        try {
            return objectMapper.readValue(json, c);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    /**
     * Convert JSON array string to List of corresponding Java objects.
     *
     * @param jsonArray JSON array string.
     * @param clazz     Type of the objects in the list.
     * @param <T>       Type of the objects.
     * @return List of corresponding Java objects or an empty list.
     */
    public static <T> List<T> parseJsonArrToList(String jsonArray, Class<T> clazz) {
        try {
            return objectMapper.readValue(jsonArray, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
        return Collections.emptyList();
    }
}
