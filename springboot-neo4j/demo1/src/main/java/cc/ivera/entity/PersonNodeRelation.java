package cc.ivera.entity;

import lombok.Data;
import org.neo4j.ogm.annotation.*;

import java.io.Serializable;

@Data
@RelationshipEntity(type = "personNodeRelation")
public class PersonNodeRelation implements Serializable {

    @Id
    @GeneratedValue
    private Long id;

    @StartNode
    private PersonNode parent;

    @EndNode
    private PersonNode child;

    @Property
    private String relation;

    public PersonNodeRelation(PersonNode parent, PersonNode child, String relation) {
        this.parent = parent;
        this.child = child;
        this.relation = relation;
    }
}

