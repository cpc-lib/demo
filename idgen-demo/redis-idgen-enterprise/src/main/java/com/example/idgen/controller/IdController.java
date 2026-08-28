package com.example.idgen.controller;

import com.example.idgen.service.IdGeneratorService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/id")
public class IdController {

    private final IdGeneratorService idService;

    public IdController(IdGeneratorService idService) {
        this.idService = idService;
    }

    /**
     * Example:
     *   GET /api/id/next/1/order
     */
    @GetMapping("/next/{tenantId}/{bizKey}")
    public Map<String, Object> next(@PathVariable Integer tenantId, @PathVariable String bizKey) {
        long id = idService.nextId(tenantId, bizKey);
        Map<String, Object> resp = new HashMap<>();
        resp.put("tenantId", tenantId);
        resp.put("bizKey", bizKey);
        resp.put("id", id);
        return resp;
    }
}
