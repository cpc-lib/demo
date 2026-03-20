package cc.ivera.factory;

import cc.ivera.service.UserService;
import cc.ivera.service.impl.UserServiceImpl;

public class StaticFactory {
    public static UserService getUserService(){
        return new UserServiceImpl();
    }
}
