package com.example.limitdemo;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class DemoController {

    private final ExternalBusinessService businessService;

    public DemoController(ExternalBusinessService businessService) {
        this.businessService = businessService;
    }

    @GetMapping("/external")
    public String callExternal() {
        return businessService.callExternal();
    }

    @GetMapping("/slow")
    public String slow() {
        return businessService.localSlowBusiness();
    }

    @GetMapping("get")
    public String echo(@RequestParam(name = "from", required = false) String from){
        return "success";
    }

    @ExceptionHandler(ConcurrencyLimitException.class)
    public ResponseEntity<String> handleLimit(ConcurrencyLimitException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(e.getMessage());
    }
}
