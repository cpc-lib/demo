package cc.ivera.dao;

import cc.ivera.entity.PersonNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonNodeRepository extends Neo4jRepository<PersonNode, Long> {

}
