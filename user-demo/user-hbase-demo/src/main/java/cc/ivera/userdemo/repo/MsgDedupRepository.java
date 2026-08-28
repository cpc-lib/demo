package cc.ivera.userdemo.repo;

import cc.ivera.userdemo.mongo.MsgDedupDoc;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MsgDedupRepository extends MongoRepository<MsgDedupDoc, String> {
  List<MsgDedupDoc> findTop200ByStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(String status, long now);
}
