package cc.ivera.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import cc.ivera.dao.BookDao;
import cc.ivera.dao.Room;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component("bookDao")
@Scope("singleton")
@PropertySource({"jdbc.properties"})
public class BookDaoImpl implements BookDao {

    @Autowired
    @Qualifier("room2")
    private Room room;

    @Value("${jdbc.username}")
    private String userName;


    @PostConstruct
    public void init(){
        System.out.println("init");
    }

    public void add() {
        System.out.println("添加"+userName+"成功!");
        room.add();
    }


    @PreDestroy
    public void destroy(){
        System.out.println("destroy");
    }

}
