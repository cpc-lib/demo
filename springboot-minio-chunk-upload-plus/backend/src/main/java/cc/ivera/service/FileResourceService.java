package cc.ivera.service;

import com.baomidou.mybatisplus.extension.service.IService;
import cc.ivera.entity.FileResource;

public interface FileResourceService extends IService<FileResource> {

    FileResource findByMd5(String md5);
}
