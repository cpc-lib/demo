package cc.ivera.test.base;

import cn.hutool.core.io.resource.ClassPathResource;
import cn.idev.excel.FastExcel;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import cc.ivera.model.pojo.easyexcel.Movie;

import java.util.ArrayList;
import java.util.List;


public class Demo85 {

    public static void main(String[] args) throws InterruptedException {

        String userAgent = "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/131.0.0.0 Mobile Safari/537.36";
        List<Movie> datas = new ArrayList<>();

        for (int i = 0; i <= 9; i++) {
            String url = String.format("https://movie.douban.com/top250?start=%d", i * 25);
            List<Movie> pageData = getPageData(url, userAgent);
            datas.addAll(pageData);
            Thread.sleep(1000);
        }
        ClassPathResource classPathResource = new ClassPathResource("easyexcel/output/test8.xlsx");
        String absolutePath = classPathResource.getFile().getAbsolutePath();
        // 向Excel中写入数据 也可以通过 head(Class<?>) 指定数据模板
        FastExcel.write(absolutePath, Movie.class).sheet("Sheet1").doWrite(datas);
    }

    private static List<Movie> getPageData(String url, String userAgent) {
        List<Movie> datas = new ArrayList<>();
        try {
            // 使用Jsoup连接到网页
            Document document = Jsoup.connect(url).userAgent(userAgent).get();
            // 获取网页标题
            //String title = document.title();
            Element body = document.body();
            Element content = body.getElementById("content");
            Elements div = content.getElementsByTag("div");
            Element element = div.get(0);
            Elements lis = element.getElementsByClass("article")
                    .get(0).getElementsByTag("ol")
                    .get(0).getElementsByTag("li");

            for (Element li : lis) {
                Movie map = new Movie();
                Element title = li.getElementsByClass("hd").get(0).getElementsByTag("span").get(0);
                map.setTitle(title.text());
                Element bd = li.getElementsByClass("bd").get(0);
                Element p = bd.getElementsByTag("p").get(0);
                map.setFullText(p.text());
                //网页页面修改获取rating_num的span标签然后获取到评分
                Element score = bd.getElementsByClass("rating_num").get(0).getElementsByAttribute("property").get(0);
                map.setScore(score.text());
                Elements quoteSizes = bd.getElementsByClass("quote");
                if (quoteSizes.size() > 0) {
                    Element quote = quoteSizes.get(0).getElementsByTag("p").get(0);
                    map.setQuote(quote.text());
                }
                datas.add(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return datas;
    }
}
