package cc.ivera.dao;

import cc.ivera.entity.PersonNode;
import cc.ivera.entity.PersonNodeRelation;
import cc.ivera.entity.Relation;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonNodeRelationRepository extends Neo4jRepository<PersonNodeRelation, Long> {

    @Query("match (n:Man),(m:manRelation),(s:Man) where n.name=m.from and" + " s.name=m.to create (n)-[:manRelationShip {relation:m.relation}]->(s)")
    void buildAllRelation();

    //指定创建关系
    @Query("match (n:PersonNode {name:{0}}),(m:PersonNode {name:{2}}) create (n)-[:r {personNodeRelation:{1}}]->(m) ")
    void buildRelation(String name, String relation, String targetName);

    @Query("MATCH (n:PersonNode)-[r:personNodeRelation]->(m:PersonNode) where n.name={0} RETURN n.name as source,r.relation as relation,m.name as target")
    List<Relation> findRelationV1(String name);

    @Query("MATCH (n:PersonNode)-[r:personNodeRelation]->(m:PersonNode) where n.name={0} RETURN m")
    List<PersonNode> findRelation(String name);
}
