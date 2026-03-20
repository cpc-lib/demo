package cc.ivera.dao.impl;

import org.springframework.stereotype.Component;
import cc.ivera.dao.Room;

@Component("room")
public class RoomImpl implements Room {
    public void add() {
        System.out.println("use room obj(room)");
    }
}
