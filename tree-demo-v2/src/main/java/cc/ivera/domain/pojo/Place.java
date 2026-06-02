package cc.ivera.domain.pojo;

import cc.ivera.util.Treeable;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Place implements Treeable {

    private Integer id;
    private Integer pid;
    private String name;
    private List<Place> children = new ArrayList<>();

    public Place() {
    }

    public Place(Integer id, Integer pid, String name) {
        this.id = id;
        this.pid = pid;
        this.name = name;
    }

    @Override
    public Integer getMapKey() {
        return pid;
    }

    @Override
    public Integer getChildrenKey() {
        return id;
    }

    @Override
    public void setChildren(List children) {
        this.children = children;
    }


    @Override
    public Integer getRootKey() {
        return (Integer) rootKey;
    }
}
