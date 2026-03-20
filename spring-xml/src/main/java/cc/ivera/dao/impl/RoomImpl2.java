package cc.ivera.dao.impl;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import cc.ivera.dao.Room;

@Component
@Primary
public class RoomImpl2 implements Room {
    public void add() {
        System.out.println("roomimpl2");
    }
}
