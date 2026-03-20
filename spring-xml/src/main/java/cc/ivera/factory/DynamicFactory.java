package cc.ivera.factory;

import cc.ivera.service.UserService;
import cc.ivera.service.impl.UserServiceImpl;

public class DynamicFactory {
    public UserService getUserService2(){
        return new UserServiceImpl();
    }
}
