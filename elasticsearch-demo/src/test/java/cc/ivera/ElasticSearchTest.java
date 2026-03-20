package cc.ivera;

import cc.ivera.config.ConfigA;
import cc.ivera.config.ConfigB;
import cc.ivera.config.ConfigC;
import cc.ivera.domain.Goods;
import cc.ivera.domain.Person;
import cc.ivera.mapper.GoodsMapper;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

@SpringBootTest
//RunWith的作用
// 参考链接：https://blog.csdn.net/weixin_43671497/article/details/90543225
//引入SpringJUnit4ClassRunner，才可以在类中进行注入对象。ioc
@RunWith(SpringJUnit4ClassRunner.class)
public class ElasticSearchTest {

    @Test
    public void test1() {
        //创建es客户端对象
        RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(
                new HttpHost(
                        "127.0.0.1",
                        9200,
                        "http"
                )
        ));
        System.out.println(client);
    }

    @Autowired
    private ConfigA configA;


    @Autowired
    private ConfigB configB;

    @Autowired
    private ConfigC configC;

    @Autowired
    private GoodsMapper goodsMapper;


    @Autowired
    private ConfigA client;

    @Autowired
    private RestHighLevelClient elasticClient;


    @Test
    public void test2() {
        System.out.println(configA.elasticClient());
    }


    @Test
    public void test3() {
        System.out.println(configB.clientB());
    }

    @Test
    public void test4(){
        System.out.println(configC.clientC());
    }

    @Test
    public void createIndex() throws IOException {
        if (client==configA){
            System.out.println("在一个类中注入相同的bean，bean只会被创创建一次");
        }
        //使用client对象获取操作索引的对象
        IndicesClient indices = client.elasticClient().indices();
        //设置索引库的名称
        CreateIndexRequest createIndexRequest = new CreateIndexRequest("jenny");
        //执行创建索引库的操作
        CreateIndexResponse response = indices.create(createIndexRequest, RequestOptions.DEFAULT);
        //打印结果，使用kibana查看索引是否创建成功
        System.out.println(response.isAcknowledged());
    }

    @Test
    public void createIndexAndMapping() throws IOException {
        //使用client获取 操作 索引的对象
        IndicesClient indices = elasticClient.indices();
        //设置索引库的名称
        CreateIndexRequest createIndexRequest = new CreateIndexRequest("tomcatf");

        //参考链接:https://blog.csdn.net/weixin_38170468/article/details/101727083
        //设置mapping
        String source="{\n" +
                "    \"_doc\" : {\n" +
                "      \"properties\" : {\n" +
                "        \"address\" : {\n" +
                "          \"type\" : \"text\",\n" +
                "          \"analyzer\" : \"ik_max_word\"\n" +
                "        },\n" +
                "        \"age\" : {\n" +
                "          \"type\" : \"integer\"\n" +
                "        },\n" +
                "        \"name\" : {\n" +
                "          \"type\" : \"keyword\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }";
        createIndexRequest.mapping("_doc",source, XContentType.JSON);
        //进行创建索引库并执行mapping的设置
        CreateIndexResponse createIndexResponse = indices.create(createIndexRequest, RequestOptions.DEFAULT);
        //操作索引库的名称
        String index = createIndexResponse.index();
        if("tomcatf".equals(index)){
            System.out.println(true);
        }
    }


    @Test
    public void queryIndex() throws IOException {
        //根据索引库获取mapping信息
        IndicesClient indices = elasticClient.indices();
        GetMappingsRequest getMappingRequest= new GetMappingsRequest().indices("person");
        GetMappingsResponse mapping = indices.getMapping(getMappingRequest, RequestOptions.DEFAULT);
        System.out.println(mapping.toString());
    }


    @Test
    public void deleteIndex() throws IOException {
        //获取操作索引的对象
        IndicesClient indices = elasticClient.indices();
        DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest().indices("newboy");
        AcknowledgedResponse response = indices.delete(deleteIndexRequest, RequestOptions.DEFAULT);
        //print the result of deleting index(true or false)
        System.out.println(response.isAcknowledged());
    }

    @Test
    public void existIndex() throws IOException {
        IndicesClient indices = elasticClient.indices();
        GetIndexRequest getIndexRequest = new GetIndexRequest().indices("person");
        boolean exists = indices.exists(getIndexRequest, RequestOptions.DEFAULT);
        System.out.println(exists);
    }

    @Test
    /*use map to add document*/
    public void addDoc() throws IOException {
        //给索引库添加文档
        IndexRequest indexRequest = new IndexRequest("person");
        Map map = new HashMap();
        map.put("name","赵六");
        map.put("age",23);
        map.put("address","广东湛江");
        //随机id
        indexRequest.source(map);
        IndexResponse response = elasticClient.index(indexRequest, RequestOptions.DEFAULT);
        System.out.println(response.getId());
    }

    @Test
    public void addDocbyJSONStr() throws IOException {
        //模拟从书库取出数据放到es中
        Person person = new Person();
        //忽略对象中某个字段转换成json
        // https://bbs.csdn.net/topics/393638425
        person.setId("4");//此字段被忽略
        person.setAge(23);
        person.setName("周七");
        person.setAddress("北京昌平");
        String json = JSON.toJSONString(person);
        IndexRequest indexRequest = new IndexRequest("person").id(person.getId()).source(json,XContentType.JSON);
        IndexResponse response = elasticClient.index(indexRequest, RequestOptions.DEFAULT);
        System.out.println(response.getId());
    }


    @Test
    /***
     * 如果id存在，进行更新文档，如果id不存在，则进行保存
     */
    public void updateDoc() throws IOException {
        //模拟从书库取出数据放到es中
        Person person = new Person();
        //忽略对象中某个字段转换成json
        // https://bbs.csdn.net/topics/393638425
        person.setId("4");//此字段被忽略
        person.setAge(24);
        person.setName("王雨婷");
        person.setAddress("广东湛江");
        String json = JSON.toJSONString(person);
        IndexRequest indexRequest = new IndexRequest("person").id(person.getId()).source(json,XContentType.JSON);
        IndexResponse response = elasticClient.index(indexRequest, RequestOptions.DEFAULT);
        System.out.println(response.getId());
    }


    @Test
    public void findDocById() throws IOException {
        GetRequest getRequest = new GetRequest("person","4");
        GetResponse response = elasticClient.get(getRequest, RequestOptions.DEFAULT);
        System.out.println(response.getSourceAsString());
    }

    @Test
    public void deleteDocById() throws IOException {
        //根据id删除文档
        DeleteRequest deleteRequest = new DeleteRequest("person").id("Bar10X4BDovupwdNP2Qn");
        DeleteResponse response = elasticClient.delete(deleteRequest, RequestOptions.DEFAULT);
        if(!response.isFragment()){
            System.out.println("数据已被删除!");
        }
    }

    @Test
    public void deleteAllDoc() throws IOException {
        BulkRequest bulkRequest = new BulkRequest();
        //1.删除id为2的文档
        DeleteRequest deleteRequest=new DeleteRequest("person").id("2");
        bulkRequest.add(deleteRequest);
        //2.添加id为9的文档
        IndexRequest indexRequest = new IndexRequest("person").id("9");
        Map map = new HashMap();
        map.put("name","李常茹");
        map.put("age",23);
        map.put("address","广东广州");
        indexRequest.source(map);
        bulkRequest.add(indexRequest);
        //3.更新id为3的文档
        UpdateRequest updateRequest = new UpdateRequest("person","3");
        String source ="{\"name\":\"李白\",\"age\":22,\"address\":\"长安\"}";
        updateRequest.doc(source,XContentType.JSON);
        bulkRequest.add(updateRequest);
        //bulkRequest.add()
        BulkResponse response = elasticClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        System.out.println(response.status());
    }

    @Test
    public void importData() throws IOException {
        //从mysql中获取到所有的数据记录
        List<Goods> goodsList = goodsMapper.findAll();
        BulkRequest bulkRequest = new BulkRequest().add();
        //使用循环遍历，为indexRequest添加数据
        for (Goods goods : goodsList) {
            goods.setSpec(JSON.parseObject(goods.getSpecStr(),Map.class));
            IndexRequest indexRequest = new IndexRequest("goods");
            indexRequest.id(goods.getId()+"").source(JSON.toJSONString(goods),XContentType.JSON);
            bulkRequest.add(indexRequest);
        }
        BulkResponse response = elasticClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        System.out.println(response.status());
    }


    @Test
    public void testMatchAll() throws IOException {

        //2.创建searchRequest对象指定操作的索引名称
        SearchRequest searchRequest = new SearchRequest("goods");
        //4.创建searchsourcebuilder对象
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //6.创建条件查询器
        QueryBuilder queryBuilder = QueryBuilders.matchAllQuery();
        //5.使用searchsourcebuilder对象进行查询
        SearchSourceBuilder query = sourceBuilder.query(queryBuilder);
        //7.进行分页操作
        query.from(0);
        query.size(100);
        //3.使用searchRequest对象.
       searchRequest.source(sourceBuilder);
        //1.获取response
        SearchResponse response = elasticClient.search(searchRequest, RequestOptions.DEFAULT);
        //8.获取hit对象
        SearchHits hits = response.getHits();
        //9.使用hit对象获取hits的内部信息
        SearchHit[] hit = hits.getHits();
        List<Goods> goodsList = new ArrayList<>();
        for (SearchHit s : hit) {
            //10.遍历，添加数据到list中。吧
            Goods goods = JSON.parseObject(s.getSourceAsString(), Goods.class);
            goodsList.add(goods);
        }
        System.out.println(goodsList.size());
    }


    @Test
    public void testTermQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest("goods");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        QueryBuilder query = QueryBuilders.termsQuery("title","华为");
        sourceBuilder.query(query);
        searchRequest.source(sourceBuilder);
        SearchResponse response = elasticClient.search(searchRequest, RequestOptions.DEFAULT);
        System.out.println("value:"+response.getHits().getTotalHits().value);
    }

    @Test
    public void testMatchQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        MatchQueryBuilder query = QueryBuilders.matchQuery("title","华为笔记本");
        //指定运算方式，默认为or
        query.operator(Operator.AND);
        sourceBuilder.query(query);
        searchRequest.source(sourceBuilder);
        SearchResponse response = elasticClient.search(searchRequest, RequestOptions.DEFAULT);
        System.out.println("value:"+response.getHits().getTotalHits().value);
    }

    @Test
    public void testWildCardQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest("goods") ;
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        QueryBuilder query = QueryBuilders.wildcardQuery("title","华*");
        sourceBuilder.query(query);
        searchRequest.source(sourceBuilder);
        SearchResponse response = elasticClient.search(searchRequest, RequestOptions.DEFAULT);
        System.out.println("value:"+response.getHits().getTotalHits().value);
    }


    @Test
    public void testRegexpQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        QueryBuilder query = QueryBuilders.regexpQuery("title","\\w+(.)*");
        sourceBuilder.query(query);
        searchRequest.source(sourceBuilder);
        SearchResponse response = elasticClient.search(searchRequest, RequestOptions.DEFAULT);
        System.out.println("value:"+response.getHits().getTotalHits().value);
    }

    @Test
    public void testPrefixQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        QueryBuilder query = QueryBuilders.prefixQuery("title","三");
        sourceBuilder.query(query);
        searchRequest.source(sourceBuilder);
        SearchResponse response = elasticClient.search(searchRequest, RequestOptions.DEFAULT);
        System.out.println("value:"+response.getHits().getTotalHits().value);
    }

    @Test
    public void testRangeQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        RangeQueryBuilder query = QueryBuilders.rangeQuery("price");
        query.gte(2000.0);
        query.lte(9999.0);

        sourceBuilder.query(query);
        sourceBuilder.from(0);
        sourceBuilder.size(2);
        sourceBuilder.sort("price", SortOrder.ASC);
        searchRequest.source(sourceBuilder);
        SearchResponse response = elasticClient.search(searchRequest, RequestOptions.DEFAULT);
        List<Goods> list=new ArrayList<>();
        SearchHit[] hits = response.getHits().getHits();
        for (SearchHit hit : hits) {
            Goods goods = JSON.parseObject(hit.getSourceAsString(), Goods.class);
            System.out.println(goods);
        }
    }


    @Test
    public void testQueryStringQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest("goods");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        QueryBuilder query = QueryBuilders.queryStringQuery("华为手机")
                .field("title").field("categoryName").field("brandName");
        sourceBuilder.query(query);
        searchRequest.source(sourceBuilder);
        SearchResponse response = elasticClient.search(searchRequest, RequestOptions.DEFAULT);
        System.out.println("value:"+response.getHits().getTotalHits().value);
    }

    @Test
    public void testBoolQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest("goods");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        RangeQueryBuilder price = QueryBuilders.rangeQuery("price").gte("2000.0").lte("9999.0");
        boolQueryBuilder.must(price);
        PrefixQueryBuilder prefix = QueryBuilders.prefixQuery("title", "三星");
        boolQueryBuilder.filter(prefix);
        sourceBuilder.query(boolQueryBuilder);
        searchRequest.source(sourceBuilder);
        SearchResponse response = elasticClient.search(searchRequest, RequestOptions.DEFAULT);
        System.out.println("value:"+response.getHits().getTotalHits().value);
    }


    @Test
    public void testFunctionQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        MatchQueryBuilder query = QueryBuilders.matchQuery("title", "手机");
        query.operator(Operator.AND);
        sourceBuilder.query(query);
        AggregationBuilder aggregation = AggregationBuilders.terms("goods_brand").field("brandName").size(100);
        sourceBuilder.aggregation(aggregation);
        searchRequest.source(sourceBuilder);
        SearchResponse response = elasticClient.search(searchRequest, RequestOptions.DEFAULT);
        Set<String> brand = new HashSet<>();
        for (SearchHit hit : response.getHits().getHits()) {
            Goods goods = JSON.parseObject(hit.getSourceAsString(), Goods.class);
            //System.out.println(goods.getBrandName());
            brand.add(goods.getBrandName());
        }
        Aggregations aggregations = response.getAggregations();
        Terms goods_brand = (Terms)aggregations.asMap().get("goods_brand");
        List<? extends Terms.Bucket> buckets = goods_brand.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            String key =  (String)bucket.getKey();
            System.out.println(key);
        }
        brand.stream().forEach(s-> System.out.println(s));
    }

    @Test
    /***
     * 高亮字段设置
     * 前缀
     * 后缀
     */
    public void testHighlightQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        QueryBuilder query = QueryBuilders.matchQuery("title","手机");
        sourceBuilder.query(query);
        //高亮查询
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title").preTags("<em color='red'>").postTags("</em>");
        //response 新增一个highlight字段
        sourceBuilder.highlighter(highlightBuilder);
        searchRequest.source(sourceBuilder);
        SearchResponse response = elasticClient.search(searchRequest, RequestOptions.DEFAULT);
        SearchHit[] hits = response.getHits().getHits();
        List<Goods> goodsList = new ArrayList<>();
        for (SearchHit hit : hits) {
            Goods goods = JSON.parseObject(hit.getSourceAsString(), Goods.class);
            HighlightField hightField = hit.getHighlightFields().get("title");
            Text[] fragments = hightField.fragments();
            //设置最新的高亮状态的title进行替换。
            goods.setTitle(fragments[0].toString());
            goodsList.add(goods);
        }
        goodsList.stream().forEach(new Consumer<Goods>() {
            @Override
            public void accept(Goods goods) {
                System.out.println(goods);
            }
        });
    }

    //TODO LEARN DOCKER
    //TODO es集群的搭建
}