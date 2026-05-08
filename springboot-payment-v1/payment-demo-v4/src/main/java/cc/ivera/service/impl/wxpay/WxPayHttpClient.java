package cc.ivera.service.impl.wxpay;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class WxPayHttpClient {

    private final CloseableHttpClient wxPayClient;

    private final CloseableHttpClient wxPayNoSignClient;

    public WxPayHttpClient(
        @Qualifier("wxPayClient") CloseableHttpClient wxPayClient,
        @Qualifier("wxPayNoSignClient") CloseableHttpClient wxPayNoSignClient
    ) {
        this.wxPayClient = wxPayClient;
        this.wxPayNoSignClient = wxPayNoSignClient;
    }

    public String get(String url, String failureMessage) throws IOException {
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Accept", "application/json");
        return executeForSuccess(wxPayClient, httpGet, failureMessage);
    }

    public String getNoSign(String url, String failureMessage) throws IOException {
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Accept", "application/json");
        return executeForSuccess(wxPayNoSignClient, httpGet, failureMessage);
    }

    public String postJson(String url, String jsonParams, String failureMessage) throws IOException {
        WxPayHttpResponse response = postJsonForResponse(url, jsonParams);
        if (!response.isSuccessful()) {
            throw new IOException(failureMessage + ", 响应码 = " + response.getStatusCode() + ", 返回结果 = " + response.getBody());
        }
        return response.getBody();
    }

    public WxPayHttpResponse postJsonForResponse(String url, String jsonParams) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        StringEntity entity = new StringEntity(jsonParams, "utf-8");
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");
        return execute(wxPayClient, httpPost);
    }

    private String executeForSuccess(CloseableHttpClient client,
                                     HttpUriRequest request,
                                     String failureMessage) throws IOException {
        WxPayHttpResponse response = execute(client, request);
        if (!response.isSuccessful()) {
            throw new IOException(failureMessage + ", 响应码 = " + response.getStatusCode() + ", 返回结果 = " + response.getBody());
        }
        return response.getBody();
    }

    private WxPayHttpResponse execute(CloseableHttpClient client, HttpUriRequest request) throws IOException {
        CloseableHttpResponse response = client.execute(request);
        try {
            int statusCode = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            String body = entity == null ? "" : EntityUtils.toString(entity);
            if (statusCode == 200) {
                log.info("微信支付请求成功, 返回结果 = {}", body);
            } else if (statusCode == 204) {
                log.info("微信支付请求成功");
            } else {
                log.info("微信支付请求失败, 响应码 = {}, 返回结果 = {}", statusCode, body);
            }
            return new WxPayHttpResponse(statusCode, body);
        } finally {
            response.close();
        }
    }

    @Getter
    public static class WxPayHttpResponse {

        private final int statusCode;

        private final String body;

        private WxPayHttpResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        public boolean isSuccessful() {
            return statusCode == 200 || statusCode == 204;
        }
    }
}
