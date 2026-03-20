package cc.ivera.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class FileIndexService {

    private final ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();

    public String getPathByMd5(String md5) { return map.get(md5); }

    public void save(String md5, String path) { map.put(md5, path); }
}
