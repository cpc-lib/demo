package com.example.fencing.web;

import com.example.fencing.model.AddRequest;
import com.example.fencing.model.CounterSnapshot;
import com.example.fencing.model.MutationResponse;
import com.example.fencing.repository.CounterRepository;
import com.example.fencing.service.RedisLockCoordinator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/counters")
public class CounterController {
    private final RedisLockCoordinator coordinator;
    private final CounterRepository counters;

    public CounterController(RedisLockCoordinator coordinator, CounterRepository counters) {
        this.coordinator = coordinator;
        this.counters = counters;
    }

    @PostMapping("/{resourceId}/add")
    public MutationResponse add(
            @PathVariable @Pattern(regexp = "[A-Za-z0-9_-]{1,64}") String resourceId,
            @RequestHeader("Idempotency-Key")
            @Pattern(regexp = "[A-Za-z0-9_-]{1,64}") String idempotencyKey,
            @Valid @RequestBody AddRequest request) {
        return coordinator.add(resourceId, idempotencyKey, request.delta());
    }

    @GetMapping("/{resourceId}")
    public ResponseEntity<CounterSnapshot> get(
            @PathVariable @Pattern(regexp = "[A-Za-z0-9_-]{1,64}") String resourceId) {
        return ResponseEntity.of(counters.find(resourceId));
    }
}
