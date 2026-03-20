package cc.ivera.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cc.ivera.entity.FileResource;
import cc.ivera.mapper.FileResourceMapper;
import cc.ivera.service.FileResourceService;
import org.springframework.stereotype.Service;

@Service
public class FileResourceServiceImpl
        extends ServiceImpl<FileResourceMapper, FileResource>
        implements FileResourceService {

    @Override
    public FileResource findByMd5(String md5) {
        return lambdaQuery().eq(FileResource::getFileMd5, md5).one();
    }
}
