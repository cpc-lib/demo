package cc.ivera.util;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static cc.ivera.util.ServletUtil.getResponse;

@Slf4j
public class Convert2Json {
    public void convert2json(Object obj) {
        String json = JSONObject.toJSONString(obj);
        HttpServletResponse response = getResponse();
        response.setContentType("application/json;charset=utf-8");
        PrintWriter out = null;
        try {
            out = response.getWriter();
            out.write(json);
        } catch (IOException e) {
            log.error("BaseAction::writerJson catch exception:", e);
        } finally {
            if (out != null)
                out.close();
        }
    }

    public void writerJson(Object obj) {
        String json = JSONObject.toJSONString(obj);
        HttpServletResponse response = getResponse();
        response.setContentType("application/json;charset=utf-8");
        PrintWriter out = null;
        try {
            out = response.getWriter();
            out.write(json);
        } catch (IOException e) {
            log.error("BaseAction::writerJson catch exception:", e);
        } finally {
            if (out != null)
                out.close();
        }
    }

    public void writerArrayJson(Object obj) {
        String json = JSONObject.toJSONString(obj);
        HttpServletResponse response = getResponse();
        response.setContentType("application/json;charset=utf-8");
        PrintWriter out = null;
        try {
            out = response.getWriter();
            out.write(json);
        } catch (IOException e) {
            log.error("BaseAction::writerJson catch exception:", e);
        } finally {
            if (out != null)
                out.close();
        }
    }


}
