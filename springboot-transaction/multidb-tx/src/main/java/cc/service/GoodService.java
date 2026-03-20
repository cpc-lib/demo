package cc.ivera.service;

import cc.ivera.domain.Good;

import java.util.Map;

public interface GoodService {

    public int insert(Good good);

    public Map<String,Object> sellGood1(String goodId);

    public Map<String, Object> sellGood2(String goodId);

    public Map<String, Object> sellGood3(String goodId) throws InterruptedException;
}
