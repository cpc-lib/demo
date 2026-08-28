package com.example.idgen.service;

import com.example.idgen.core.RedisSegmentAllocator;
import com.example.idgen.core.SegmentBuffer;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IdGeneratorService {

    private final RedisSegmentAllocator allocator;
    private final Map<String, SegmentBuffer> buffers = new ConcurrentHashMap<>();
    private final int step = 20000;

    public IdGeneratorService(RedisSegmentAllocator allocator) {
        this.allocator = allocator;
    }

    public long nextId(String bizKey) {
        SegmentBuffer buffer = buffers.computeIfAbsent(bizKey, k -> new SegmentBuffer());
        return buffer.nextId(() -> allocator.allocate(bizKey, step), step);
    }
}
