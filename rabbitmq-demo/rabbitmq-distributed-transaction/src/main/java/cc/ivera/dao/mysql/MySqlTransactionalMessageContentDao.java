package cc.ivera.dao.mysql;

import cc.ivera.dao.TransactionalMessageContentDao;
import cc.ivera.entity.TransactionalMessageContent;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * @version v1.0
 * @description
 * @since 2020/2/4 23:32
 */
@RequiredArgsConstructor
@Repository
public class MySqlTransactionalMessageContentDao implements TransactionalMessageContentDao {

    private static final ResultSetConverter<TransactionalMessageContent> CONVERTER = r -> {
        TransactionalMessageContent content = new TransactionalMessageContent();
        content.setId(r.getLong("id"));
        content.setMessageId(r.getLong("message_id"));
        content.setContent(r.getString("content"));
        return content;
    };
    private static final ResultSetExtractor<List<TransactionalMessageContent>> MULTI = r -> {
        List<TransactionalMessageContent> list = new ArrayList<>();
        while (r.next()) {
            list.add(CONVERTER.convert(r));
        }
        return list;
    };
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void insert(TransactionalMessageContent record) {
        jdbcTemplate.update("INSERT INTO t_transactional_message_content(message_id, content) VALUES (?,?)",
                p -> {
                    p.setLong(1, record.getMessageId());
                    p.setString(2, record.getContent());
                });
    }

    @Override
    public List<TransactionalMessageContent> queryByMessageIds(String messageIds) {
        return jdbcTemplate.query("SELECT * FROM t_transactional_message_content WHERE message_id IN " + messageIds, MULTI);
    }
}
