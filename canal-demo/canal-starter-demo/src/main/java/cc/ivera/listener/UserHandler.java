package cc.ivera.listener;

import cc.ivera.model.User;
import org.springframework.stereotype.Component;
import top.javatool.canal.client.handler.EntryHandler; // 以仓库实际包名为准
import top.javatool.canal.client.annotation.CanalTable;

@CanalTable(value = "t_user") // 如果实体没@Table，可在handler上标表名
@Component
public class UserHandler implements EntryHandler<User> {

    @Override
    public void insert(User user) {
        System.out.println("INSERT: " + user.toString());
    }

    @Override
    public void update(User before, User after) {
        // README说明：before 只包含变更字段；after 包含所有字段
        System.out.println("UPDATE before=" + before.toString());
        System.out.println("UPDATE after=" + after.toString());
    }

    @Override
    public void delete(User user) {
        System.out.println("DELETE: " + user.toString());
    }
}
