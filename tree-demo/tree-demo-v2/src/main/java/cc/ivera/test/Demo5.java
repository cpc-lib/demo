package cc.ivera.test;

import cc.ivera.domain.pojo.Place;
import cc.ivera.util.TreeUtil;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class Demo5 {

    private static List<Place> placeList = new ArrayList<Place>();

    //模拟从数据库取出数据
    static {
        placeList.add(new Place(1, null, "中华人民共和国"));

        placeList.add(new Place(2, 1, "湖南省"));
        placeList.add(new Place(3, 2, "长沙市"));
        placeList.add(new Place(100, 3, "长沙市-岳麓区"));
        placeList.add(new Place(100, 3, "长沙市-雨花台区"));
        placeList.add(new Place(1001, 100, "长沙市-雨花台区—某超市"));
        placeList.add(new Place(4, 2, "株洲市"));
        placeList.add(new Place(5, 2, "湘潭市"));
        placeList.add(new Place(6, 2, "衡阳市"));
        placeList.add(new Place(7, 2, "邵阳市"));
        placeList.add(new Place(8, 2, "岳阳市"));
        placeList.add(new Place(9, 2, "常德市"));
        placeList.add(new Place(10, 2, "张家界市"));
        placeList.add(new Place(11, 2, "益阳市"));
        placeList.add(new Place(12, 2, "郴州市"));
        placeList.add(new Place(13, 2, "永州市"));
        placeList.add(new Place(14, 2, "怀化市"));
        placeList.add(new Place(15, 2, "娄底市"));
        placeList.add(new Place(16, 2, "湘西土家族苗族自治州"));

        placeList.add(new Place(17, 1, "广东省"));
        placeList.add(new Place(18, 17, "广州市"));
        placeList.add(new Place(19, 17, "深圳市"));
        placeList.add(new Place(20, 17, "佛山市"));
        placeList.add(new Place(21, 17, "东莞市"));
        placeList.add(new Place(22, 17, "中山市"));
        placeList.add(new Place(23, 17, "珠海市"));
        placeList.add(new Place(24, 17, "江门市"));
        placeList.add(new Place(25, 17, "肇庆市"));
        placeList.add(new Place(26, 17, "惠州市"));
        placeList.add(new Place(27, 17, "汕头市"));
        placeList.add(new Place(28, 17, "潮州市"));
        placeList.add(new Place(29, 17, "揭阳市"));
        placeList.add(new Place(30, 17, "汕尾市"));
        placeList.add(new Place(31, 17, "湛江市"));
        placeList.add(new Place(32, 17, "茂名市"));
        placeList.add(new Place(33, 17, "阳江市"));
        placeList.add(new Place(34, 17, "云浮市"));
        placeList.add(new Place(35, 17, "韶关市"));
        placeList.add(new Place(36, 17, "清远市"));
        placeList.add(new Place(37, 17, "梅州市"));
        placeList.add(new Place(38, 17, "河源市"));


        placeList.add(new Place(39, 1, "江苏省"));
        placeList.add(new Place(40, 39, "南京市"));
        placeList.add(new Place(41, 39, "无锡市"));
        placeList.add(new Place(42, 39, "徐州市"));
        placeList.add(new Place(43, 39, "常州市"));
        placeList.add(new Place(44, 39, "苏州市"));
        placeList.add(new Place(45, 39, "南通市"));
        placeList.add(new Place(46, 39, "连云港市"));
        placeList.add(new Place(47, 39, "扬州市"));
        placeList.add(new Place(48, 39, "盐城市"));
        placeList.add(new Place(49, 39, "镇江市"));
        placeList.add(new Place(50, 39, "泰州市"));
        placeList.add(new Place(51, 39, "宿迁市"));
        placeList.add(new Place(52, 39, "江阴市"));
        placeList.add(new Place(53, 39, "宜兴市"));
        placeList.add(new Place(54, 39, "新沂市"));
        placeList.add(new Place(55, 39, "邳州市"));
        placeList.add(new Place(56, 39, "溧阳市"));
        placeList.add(new Place(57, 39, "常熟市"));
        placeList.add(new Place(58, 39, "张家港市"));
        placeList.add(new Place(59, 39, "昆山市"));
        placeList.add(new Place(60, 39, "太仓市"));
        placeList.add(new Place(62, 39, "启东市"));
        placeList.add(new Place(63, 39, "如皋市"));
        placeList.add(new Place(64, 39, "海安市"));
        placeList.add(new Place(65, 39, "东台市"));
        placeList.add(new Place(66, 39, "仪征市"));
        placeList.add(new Place(67, 39, "高邮市"));
        placeList.add(new Place(68, 39, "丹阳市"));
        placeList.add(new Place(69, 39, "扬中市"));
        placeList.add(new Place(70, 39, "句容市"));
        placeList.add(new Place(71, 39, "兴化市"));
        placeList.add(new Place(72, 39, "靖江市"));
        placeList.add(new Place(73, 39, "泰兴市"));
    }

    public List<Place> load() {
        return placeList;
    }

    @Test
    public void test() {
        List tree = TreeUtil.toTree(load(), null, 1, 1);
        System.out.println(tree);
    }


}
