package cc.ivera.util;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.write.metadata.WriteSheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

public class EasyExcelUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(EasyExcelUtil.class);

    public static <T> List<T> read(String filePath, final Class<?> clazz) {
        File f = new File(filePath);
        try (FileInputStream fis = new FileInputStream(f)) {
            return read(fis, clazz);
        } catch (FileNotFoundException e) {
            LOGGER.error("文件{}不存在", filePath, e);
        } catch (IOException e) {
            LOGGER.error("文件读取出错", e);
        }
        return null;
    }

    public static <T> List<T> read(InputStream inputStream, final Class<?> clazz) {
        if (inputStream == null) {
            throw new RuntimeException("解析出错了，文件流是null");
        }
        // 有个很重要的点 DataListener 不能被spring管理，要每次读取excel都要new,然后里面用到spring可以构造方法传进去
        DataListener<T> listener = new DataListener<>(clazz);
        // 这里 需要指定读用哪个class去读，然后读取第一个sheet 文件流会自动关闭
        EasyExcel.read(inputStream, clazz, listener).sheet().doRead();
        return listener.getRows();
    }

    public static void write(String outFile, List<?> list) {
        Class<?> clazz = list.get(0).getClass();
        EasyExcel.write(outFile, clazz).sheet().doWrite(list);
    }

    public static void write(String outFile, List<?> list, String sheetName) {
        Class<?> clazz = list.get(0).getClass();
        EasyExcel.write(outFile, clazz).sheet(sheetName).doWrite(list);
    }

    public static void write(OutputStream outputStream, List<?> list, String sheetName) {
        Class<?> clazz = list.get(0).getClass();
        // sheetName为sheet的名字，默认写第一个sheet
        EasyExcel.write(outputStream, clazz).sheet(sheetName).doWrite(list);
    }

    /**
     * 文件下载（失败了会返回一个有部分数据的Excel），用于直接把excel返回到浏览器下载
     */
    public static void download(HttpServletResponse response, List<?> list, String sheetName) throws Exception {
        if(list.size() > 0){
            Class<?> clazz = list.get(0).getClass();
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            // 这里URLEncoder.encode可以防止中文乱码 当然和easyexcel没有关系
            String fileName = URLEncoder.encode(sheetName, "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
            EasyExcel.write(response.getOutputStream(), clazz).head(clazz).sheet(sheetName).doWrite(list);
        }else{
            throw new RuntimeException("未查询到数据！");
        }
    }

    /***
     *
     * ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream(), ConsumeRecords.class)
     * .registerConverter(new MealTypeConverter()).includeColumnFieldNames(includeColumns).build();
     *
     * @param fileName
     * @param response
     * @param clazz
     * @param dataList
     * @param excelWriter 可以设置转换器,字段是否包含等
     * @param pageSize 设置多少条数据进行分页
     * @throws IOException
     */
    public static void download(String fileName, HttpServletResponse response, Class clazz,
                       List dataList, ExcelWriter excelWriter,int pageSize) throws IOException {
        fileName= URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
        response.setContentType("multipart/form-data");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        int totalPage = dataList.size() / pageSize + (dataList.size()) % pageSize == 0 ?
                dataList.size() / pageSize + 0 : dataList.size() / pageSize + 1;

        for (int i = 1; i <= totalPage; i++) {
            int skipNum = pageSize * (i - 1);
            List collect = (List)dataList.stream().skip(skipNum).limit(pageSize).collect(Collectors.toList());
            WriteSheet mainSheet = EasyExcel.writerSheet(i, "sheet" + i).head(clazz).build();
            excelWriter.write(collect, mainSheet);
        }

        excelWriter.finish();

    }

}

class DataListener<T> extends AnalysisEventListener<T> {

    Class clazz;

    DataListener(Class clazz){
        this.clazz = clazz;
    }


    private static final Logger LOGGER = LoggerFactory.getLogger(DataListener.class);

    private final List<T> rows = new ArrayList<>();

    @Override
    public void invoke(T t, AnalysisContext analysisContext) {
        rows.add(t);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        LOGGER.info("解析完成！读取{}行", rows.size());
    }

    public List<T> getRows() {
        return rows;
    }

    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        List<String> voHead = new ArrayList<>();
        Field[] fields = clazz.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            ExcelProperty myField2 = fields[i].getAnnotation(ExcelProperty.class);
            voHead.add(myField2.value()[0]);
        }
        int i=0;
        for(Integer key : headMap.keySet()){
            if(headMap.get(key) == null ){
                throw new RuntimeException("第"+(i+1)+"列表头格式不正确！");
            }
            if(i == voHead.size()){
                throw new RuntimeException("上传文件表头超过模板列数，正确表头为："+voHead);
            }
            if(!headMap.get(key).equals(voHead.get(i))){
                throw new RuntimeException("第"+(i+1)+"列【"+headMap.get(key)+"】表头模板顺序不符合，正确表头顺序为："+voHead);
            }
            i++;
        }
    }
}

