package cc.ivera.util;

import cn.hutool.core.io.resource.ClassPathResource;

import java.io.*;

public class ResourceUtil {

    public void getResource(String fileName) throws IOException {
        //关键点ClassPathResource
        ClassPathResource classPathResource = new ClassPathResource(fileName);
        printFileContent(new FileInputStream(classPathResource.getFile()));
    }

    public static void main(String[] args) throws IOException {
        new ResourceUtil().getResource("config/test.properties");
    }


    public static void printFileContent(Object obj) throws IOException {
        if (null == obj) {
            throw new RuntimeException("参数为空");
        }
        BufferedReader reader = null;
        // 如果是文件路径
        if (obj instanceof String) {
            reader = new BufferedReader(new FileReader(new File((String) obj)));
            // 如果是文件输入流
        } else
            if (obj instanceof InputStream) {
                reader = new BufferedReader(new InputStreamReader((InputStream) obj));
            }
        String line = null;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
        reader.close();
    }

}
