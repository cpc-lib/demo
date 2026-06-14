package cc.ivera.test;

import org.junit.Test;


//@RunWith(SpringRunner.class)
//@SpringBootTest(classes = Application.class)
public class Demo1 {

//    @Autowired
//    private JSONObject jsonObject;

    @Test
    public void test1() {
        String result = """
                {"data": [
                        {
                            "id": 1,
                            "pid": null,
                            "name": "中华人民共和国",
                            "children": [
                                {
                                    "id": 2,
                                    "pid": 1,
                                    "name": "湖南省",
                                    "children": [
                                        {
                                            "id": 3,
                                            "pid": 2,
                                            "name": "长沙市",
                                            "children": []
                                        },
                                        {
                                            "id": 4,
                                            "pid": 2,
                                            "name": "株洲市",
                                            "children": []
                                        },
                                        {
                                            "id": 5,
                                            "pid": 2,
                                            "name": "湘潭市",
                                            "children": []
                                        },
                                        {
                                            "id": 6,
                                            "pid": 2,
                                            "name": "衡阳市",
                                            "children": []
                                        },
                                        {
                                            "id": 7,
                                            "pid": 2,
                                            "name": "邵阳市",
                                            "children": []
                                        },
                                        {
                                            "id": 8,
                                            "pid": 2,
                                            "name": "岳阳市",
                                            "children": []
                                        },
                                        {
                                            "id": 9,
                                            "pid": 2,
                                            "name": "常德市",
                                            "children": []
                                        },
                                        {
                                            "id": 10,
                                            "pid": 2,
                                            "name": "张家界市",
                                            "children": []
                                        },
                                        {
                                            "id": 11,
                                            "pid": 2,
                                            "name": "益阳市",
                                            "children": []
                                        },
                                        {
                                            "id": 12,
                                            "pid": 2,
                                            "name": "郴州市",
                                            "children": []
                                        },
                                        {
                                            "id": 13,
                                            "pid": 2,
                                            "name": "永州市",
                                            "children": []
                                        },
                                        {
                                            "id": 14,
                                            "pid": 2,
                                            "name": "怀化市",
                                            "children": []
                                        },
                                        {
                                            "id": 15,
                                            "pid": 2,
                                            "name": "娄底市",
                                            "children": []
                                        },
                                        {
                                            "id": 16,
                                            "pid": 2,
                                            "name": "湘西土家族苗族自治州",
                                            "children": []
                                        }
                                    ]
                                },
                                {
                                    "id": 17,
                                    "pid": 1,
                                    "name": "广东省",
                                    "children": [
                                        {
                                            "id": 18,
                                            "pid": 17,
                                            "name": "广州市",
                                            "children": []
                                        },
                                        {
                                            "id": 19,
                                            "pid": 17,
                                            "name": "深圳市",
                                            "children": []
                                        },
                                        {
                                            "id": 20,
                                            "pid": 17,
                                            "name": "佛山市",
                                            "children": []
                                        },
                                        {
                                            "id": 21,
                                            "pid": 17,
                                            "name": "东莞市",
                                            "children": []
                                        },
                                        {
                                            "id": 22,
                                            "pid": 17,
                                            "name": "中山市",
                                            "children": []
                                        },
                                        {
                                            "id": 23,
                                            "pid": 17,
                                            "name": "珠海市",
                                            "children": []
                                        },
                                        {
                                            "id": 24,
                                            "pid": 17,
                                            "name": "江门市",
                                            "children": []
                                        },
                                        {
                                            "id": 25,
                                            "pid": 17,
                                            "name": "肇庆市",
                                            "children": []
                                        },
                                        {
                                            "id": 26,
                                            "pid": 17,
                                            "name": "惠州市",
                                            "children": []
                                        },
                                        {
                                            "id": 27,
                                            "pid": 17,
                                            "name": "汕头市",
                                            "children": []
                                        },
                                        {
                                            "id": 28,
                                            "pid": 17,
                                            "name": "潮州市",
                                            "children": []
                                        },
                                        {
                                            "id": 29,
                                            "pid": 17,
                                            "name": "揭阳市",
                                            "children": []
                                        },
                                        {
                                            "id": 30,
                                            "pid": 17,
                                            "name": "汕尾市",
                                            "children": []
                                        },
                                        {
                                            "id": 31,
                                            "pid": 17,
                                            "name": "湛江市",
                                            "children": []
                                        },
                                        {
                                            "id": 32,
                                            "pid": 17,
                                            "name": "茂名市",
                                            "children": []
                                        },
                                        {
                                            "id": 33,
                                            "pid": 17,
                                            "name": "阳江市",
                                            "children": []
                                        },
                                        {
                                            "id": 34,
                                            "pid": 17,
                                            "name": "云浮市",
                                            "children": []
                                        },
                                        {
                                            "id": 35,
                                            "pid": 17,
                                            "name": "韶关市",
                                            "children": []
                                        },
                                        {
                                            "id": 36,
                                            "pid": 17,
                                            "name": "清远市",
                                            "children": []
                                        },
                                        {
                                            "id": 37,
                                            "pid": 17,
                                            "name": "梅州市",
                                            "children": []
                                        },
                                        {
                                            "id": 38,
                                            "pid": 17,
                                            "name": "河源市",
                                            "children": []
                                        }
                                    ]
                                },
                                {
                                    "id": 39,
                                    "pid": 1,
                                    "name": "江苏省",
                                    "children": [
                                        {
                                            "id": 40,
                                            "pid": 39,
                                            "name": "南京市",
                                            "children": []
                                        },
                                        {
                                            "id": 41,
                                            "pid": 39,
                                            "name": "无锡市",
                                            "children": []
                                        },
                                        {
                                            "id": 42,
                                            "pid": 39,
                                            "name": "徐州市",
                                            "children": []
                                        },
                                        {
                                            "id": 43,
                                            "pid": 39,
                                            "name": "常州市",
                                            "children": []
                                        },
                                        {
                                            "id": 44,
                                            "pid": 39,
                                            "name": "苏州市",
                                            "children": []
                                        },
                                        {
                                            "id": 45,
                                            "pid": 39,
                                            "name": "南通市",
                                            "children": []
                                        },
                                        {
                                            "id": 46,
                                            "pid": 39,
                                            "name": "连云港市",
                                            "children": []
                                        },
                                        {
                                            "id": 47,
                                            "pid": 39,
                                            "name": "扬州市",
                                            "children": []
                                        },
                                        {
                                            "id": 48,
                                            "pid": 39,
                                            "name": "盐城市",
                                            "children": []
                                        },
                                        {
                                            "id": 49,
                                            "pid": 39,
                                            "name": "镇江市",
                                            "children": []
                                        },
                                        {
                                            "id": 50,
                                            "pid": 39,
                                            "name": "泰州市",
                                            "children": []
                                        },
                                        {
                                            "id": 51,
                                            "pid": 39,
                                            "name": "宿迁市",
                                            "children": []
                                        },
                                        {
                                            "id": 52,
                                            "pid": 39,
                                            "name": "江阴市",
                                            "children": []
                                        },
                                        {
                                            "id": 53,
                                            "pid": 39,
                                            "name": "宜兴市",
                                            "children": []
                                        },
                                        {
                                            "id": 54,
                                            "pid": 39,
                                            "name": "新沂市",
                                            "children": []
                                        },
                                        {
                                            "id": 55,
                                            "pid": 39,
                                            "name": "邳州市",
                                            "children": []
                                        },
                                        {
                                            "id": 56,
                                            "pid": 39,
                                            "name": "溧阳市",
                                            "children": []
                                        },
                                        {
                                            "id": 57,
                                            "pid": 39,
                                            "name": "常熟市",
                                            "children": []
                                        },
                                        {
                                            "id": 58,
                                            "pid": 39,
                                            "name": "张家港市",
                                            "children": []
                                        },
                                        {
                                            "id": 59,
                                            "pid": 39,
                                            "name": "昆山市",
                                            "children": []
                                        },
                                        {
                                            "id": 60,
                                            "pid": 39,
                                            "name": "太仓市",
                                            "children": []
                                        },
                                        {
                                            "id": 62,
                                            "pid": 39,
                                            "name": "启东市",
                                            "children": []
                                        },
                                        {
                                            "id": 63,
                                            "pid": 39,
                                            "name": "如皋市",
                                            "children": []
                                        },
                                        {
                                            "id": 64,
                                            "pid": 39,
                                            "name": "海安市",
                                            "children": []
                                        },
                                        {
                                            "id": 65,
                                            "pid": 39,
                                            "name": "东台市",
                                            "children": []
                                        },
                                        {
                                            "id": 66,
                                            "pid": 39,
                                            "name": "仪征市",
                                            "children": []
                                        },
                                        {
                                            "id": 67,
                                            "pid": 39,
                                            "name": "高邮市",
                                            "children": []
                                        },
                                        {
                                            "id": 68,
                                            "pid": 39,
                                            "name": "丹阳市",
                                            "children": []
                                        },
                                        {
                                            "id": 69,
                                            "pid": 39,
                                            "name": "扬中市",
                                            "children": []
                                        },
                                        {
                                            "id": 70,
                                            "pid": 39,
                                            "name": "句容市",
                                            "children": []
                                        },
                                        {
                                            "id": 71,
                                            "pid": 39,
                                            "name": "兴化市",
                                            "children": []
                                        },
                                        {
                                            "id": 72,
                                            "pid": 39,
                                            "name": "靖江市",
                                            "children": []
                                        },
                                        {
                                            "id": 73,
                                            "pid": 39,
                                            "name": "泰兴市",
                                            "children": []
                                        }
                                    ]
                                }
                            ]
                        }
                    ]}        
                """;


        System.out.println(result);
        //用来存储所有的Place信息
//        List<Place> list = new ArrayList<>();
//
//        jsonObject = this.jsonObject.parseObject(result);
//
//        //特殊化处理第一个元素
//        JSONArray data = jsonObject.getJSONArray("data");
//        jsonObject = (JSONObject)data.get(0);
//        Place place = JSONObject.parseObject(jsonObject.toJSONString(), Place.class);
//        place.setChildren(null);
//        list.add(place);
//        List<Place> children = TreeUtil.recursive(jsonObject.getJSONArray("children"), jsonObject, list, Place.class);
//        System.out.println(children);
    }
}
