package cc.ivera.service;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface LogService {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void logInfo(String outName,String inName,Integer money,String dateStr, Boolean flag);
}
