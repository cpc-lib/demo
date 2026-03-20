package cc.ivera.service.impl;

import org.springframework.stereotype.Service;
import cc.ivera.service.PasswordService3;

@Service
public class PasswordService3Impl implements PasswordService3 {
    @Override
    public boolean openURL(String url, String passwrod) {
        return "root".equals(passwrod);
    }
}
