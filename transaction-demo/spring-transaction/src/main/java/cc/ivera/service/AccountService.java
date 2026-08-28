package cc.ivera.service;

import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

public interface AccountService {
    @Transactional(rollbackFor = IOException.class)
   void transfter(String inName,String outName,Integer money) throws IOException;
}
