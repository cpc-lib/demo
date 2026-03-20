package cc.ivera;

import cc.ivera.dao.PersonNodeRelationRepository;
import cc.ivera.dao.PersonNodeRepository;
import cc.ivera.entity.PersonNode;
import cc.ivera.entity.PersonNodeRelation;
import cc.ivera.entity.Relation;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

@SpringBootTest
@Slf4j
class DemoApplicationTests {


    @Autowired
    private PersonNodeRepository personNodeRepository;

    @Autowired
    private PersonNodeRelationRepository personNodeRelationRepository;


    @Test
    public void test1() {

        PersonNode p1 = new PersonNode();
        PersonNode p2 = new PersonNode();
        PersonNode p3 = new PersonNode();
        PersonNode p4 = new PersonNode();
        PersonNode p5 = new PersonNode();

        p1.setName("汪峰");
        p2.setName("齐丹");
        p3.setName("葛荟婕");
        p4.setName("康作");
        p5.setName("章子怡");

        personNodeRepository.save(p1);
        personNodeRepository.save(p2);
        personNodeRepository.save(p3);
        personNodeRepository.save(p4);
        personNodeRepository.save(p5);

        PersonNodeRelation pr1 = new PersonNodeRelation(p1, p2, "第一任");
        PersonNodeRelation pr2 = new PersonNodeRelation(p1, p3, "第二任");
        PersonNodeRelation pr3 = new PersonNodeRelation(p1, p4, "第三任");
        PersonNodeRelation pr4 = new PersonNodeRelation(p1, p5, "第四任");

        personNodeRelationRepository.save(pr1);
        personNodeRelationRepository.save(pr2);
        personNodeRelationRepository.save(pr3);
        personNodeRelationRepository.save(pr4);

    }


    @Test
    public void test2() {

        PersonNode p = new PersonNode();

        p.setName("李巧");

        Optional<PersonNode> node = personNodeRepository.findById(255L);

        PersonNode personNode = node.orElse(null);

        if (personNode != null) {
            personNodeRepository.save(p);

            PersonNodeRelation pr = new PersonNodeRelation(personNode, p, "第五任");

            personNodeRelationRepository.save(pr);

        }
    }

    @Test
    public void test3() {
        personNodeRelationRepository.buildAllRelation();
    }


    @Test
    public void test4() {
        personNodeRelationRepository.buildRelation("章子怡", "前夫", "汪峰");
    }


    @Test
    public void test5() {
        List<PersonNode> relations = personNodeRelationRepository.findRelation("汪峰");
        System.out.println(relations.size());
        for (PersonNode relation : relations) {
            System.out.println(relation);
        }
    }

    //查询有点问题
    @Test
    public void test6() {
        List<Relation> relations = personNodeRelationRepository.findRelationV1("汪峰");
        System.out.println(relations.size());
        for (Relation relation : relations) {
            System.out.println(relation);
        }
    }

}
