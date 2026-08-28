package cc.ivera.userdemo.controller;

import cc.ivera.userdemo.repo.ManualTaskRepository;
import cc.ivera.userdemo.service.EsUserIndexService;
import cc.ivera.userdemo.service.HBaseUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final EsUserIndexService es;
    private final ManualTaskRepository manualRepo;
    private final HBaseUserService hbase;

    @GetMapping("/users/search")
    public Map<String, Object> search(@RequestParam(required = false) String q,
                                      @RequestParam(required = false) String phone,
                                      @RequestParam(required = false) String email,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size) throws Exception {
        return es.search(q, phone, email, page, size);
    }

    @GetMapping("/manual-tasks")
    public Map<String, Object> manualTasks(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        // 简化：不做分页游标，demo 返回前 N 条
        var list = manualRepo.findTop200ByStatusOrderByCreatedAtAsc("PENDING");
//    HashMap<String, Object>  map = new HashMap<>();
//    map.put("total",list.size());
//    map.put("items",list);
        return Map.of("total", list.size(), "items", list);
    }

    @PostMapping("/manual-tasks/{eventId}/fix")
    public Map<String, Object> fix(@PathVariable String eventId) throws Exception {
        var task = manualRepo.findById(eventId).orElseThrow(() -> new IllegalArgumentException("task not found"));
        var pOpt = hbase.getUser(task.getUid());
        if (pOpt.isEmpty()) throw new IllegalArgumentException("user not found in hbase");

        es.upsert(pOpt.get());
        task.setStatus("FIXED");
        task.setAttemptCount(task.getAttemptCount() + 1);
        task.setLastAttemptAt(System.currentTimeMillis());
        task.setUpdatedAt(System.currentTimeMillis());
        manualRepo.save(task);

        return Map.of("eventId", eventId, "status", "FIXED");
    }
}
