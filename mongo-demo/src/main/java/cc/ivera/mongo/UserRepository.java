package cc.ivera.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<UserDoc, String> {
    boolean existsByUid(String uid);
    Optional<UserDoc> findByUid(String uid);
}
