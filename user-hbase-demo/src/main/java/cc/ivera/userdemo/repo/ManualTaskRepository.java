package cc.ivera.userdemo.repo;

import cc.ivera.userdemo.mongo.ManualTaskDoc;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ManualTaskRepository extends MongoRepository<ManualTaskDoc, String> {
  List<ManualTaskDoc> findTop200ByStatusOrderByCreatedAtAsc(String status);
}
