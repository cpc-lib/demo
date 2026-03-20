package com.example.idgen.controller;

import com.example.idgen.service.IdGeneratorService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/id")
public class IdController {

    private final IdGeneratorService idService;

    public IdController(IdGeneratorService idService) {
        this.idService = idService;
    }

    @GetMapping("/{bizKey}")
    public Map<String, Object> next(@PathVariable String bizKey) {
        long id = idService.nextId(bizKey);
        Map<String, Object> map = new HashMap<>();
        map.put("bizKey", bizKey);
        map.put("id", id);
        return map;
    }
}
