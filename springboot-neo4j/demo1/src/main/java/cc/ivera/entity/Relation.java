package cc.ivera.entity;

import lombok.Data;
import org.neo4j.ogm.annotation.*;

import java.io.Serializable;

@Data
public class Relation implements Serializable {
    @Property
    private String source;
    @Property
    private String relation;
    @Property
    private String target;
}
