package cc.ivera.controller;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.aigc.generation.models.QwenParam;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.fastjson.JSONObject;
import io.reactivex.Flowable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(value = "/ai/tongyi")
public class TongYiController {

    @Value("${spring.ai.tongyi.ai-api-key}")
    private String appKey;
    @Resource
    private Generation generation;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(@RequestBody String question, HttpServletResponse response)
            throws NoApiKeyException, InputRequiredException {
        Message message = Message.builder()
                .role(Role.USER.getValue())
                .content(question).build();

        QwenParam qwenParam = QwenParam.builder()
                .model(Generation.Models.QWEN_PLUS)
                .messages(Collections.singletonList(message))
                .topP(0.8)
                .resultFormat(QwenParam.ResultFormat.MESSAGE)
                .enableSearch(true)
                .apiKey(appKey)
                .incrementalOutput(true)
                .build();
        Flowable<GenerationResult> result = generation.streamCall(qwenParam);


        return Flux.from(result)
                .map(m -> {
                    // GenerationResult对象中输出流(GenerationOutput)的choices是一个列表，存放着生成的数据。
                    String content = m.getOutput().getChoices().get(0).getMessage().getContent();
                    return ServerSentEvent.<String>builder().data(content).build();
                })
                .publishOn(Schedulers.boundedElastic())
                .doOnError(e -> {
                    Map<String, Object> map = new HashMap<>() {{
                        put("code", "400");
                        put("message", "出现了异常，请稍后重试");
                    }};
                    try {
                        response.getOutputStream().print(JSONObject.toJSONString(map));
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                });
    }
}
