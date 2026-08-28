package com.example.points.web;

import com.example.points.messaging.PointRewardProducer;
import com.example.points.repository.FailureRepository;
import com.example.points.repository.QueryRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ManagementController {
    private final QueryRepository q;
    private final FailureRepository failures;
    private final PointRewardProducer producer;

    public ManagementController(QueryRepository q, FailureRepository failures, PointRewardProducer producer) {
        this.q = q;
        this.failures = failures;
        this.producer = producer;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return q.dashboard(date == null ? LocalDate.now() : date);
    }

    @GetMapping("/batches")
    public List<Map<String, Object>> batches(@RequestParam(defaultValue = "50") int limit) {
        return q.batches(Math.min(limit, 200));
    }

    @GetMapping("/ledgers")
    public List<Map<String, Object>> ledgers(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @RequestParam(required = false) Long userId, @RequestParam(defaultValue = "100") int limit) {
        return q.ledgers(date, userId, Math.min(limit, 500));
    }

    @GetMapping("/failures")
    public List<Map<String, Object>> failures(@RequestParam(defaultValue = "100") int limit) {
        return q.failures(Math.min(limit, 500));
    }

    @PostMapping("/failures/{id}/retry")
    public ResponseEntity<Map<String, Object>> retry(@PathVariable long id) {
        var c = failures.findCommand(id);
        failures.markRetrying(id);
        producer.send(c);
        return ResponseEntity.accepted().body(Map.of("accepted", true, "bizNo", c.bizNo()));
    }

    @GetMapping("/reconciliation")
    public Map<String, Object> reconciliation(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return q.reconciliation(date, 10L);
    }

    @GetMapping("/reconciliation/missing")
    public List<Map<String, Object>> missing(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @RequestParam(defaultValue = "100") int limit) {
        return q.missing(date, Math.min(limit, 500));
    }
}
