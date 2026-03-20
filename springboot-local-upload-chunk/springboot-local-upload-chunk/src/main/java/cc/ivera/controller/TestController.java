package cc.ivera.controller;

import cc.ivera.response.RestApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/test")
public class TestController {

    @GetMapping(value = "/error")
    public RestApiResponse<String> error() {
        return RestApiResponse.success();
    }
}
