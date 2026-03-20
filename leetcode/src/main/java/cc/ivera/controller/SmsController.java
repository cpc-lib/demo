package cc.ivera.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import cc.ivera.service.SmsService;

import java.util.HashMap;
import java.util.Map;


@Controller
@RequestMapping("/sms")
@CrossOrigin
public class SmsController {

    @Autowired
    private SmsService smsService;

    @PostMapping("/sendVerification")
    @ResponseBody
    public String sendCode(@RequestParam("phoneNumber") String phoneNumber) {
        smsService.sendVerificationCode(phoneNumber);
        return "please check your phone's message!";
    }


    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity login(@RequestBody Map map) {
        String phoneNumber = map.get("phoneNumber").toString();
        String verificationCode = map.get("verificationCode").toString();
        return ResponseEntity.ok(smsService.login(phoneNumber, verificationCode));
    }

    @GetMapping("/greet")
    public ResponseEntity greet(String name) {
        Map map = new HashMap();
        map.put("message", name + "https://www.bilibili.com");
        return ResponseEntity.ok(map);
    }


}
