package cc.ivera.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import cc.ivera.model.vo.AjaxResult;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;

@Slf4j
public class FileUtil {
    //private static final Logger log = LoggerFactory.getLogger(FileUtil.class);

    private static final String uploadDir = "/upload";

    public static void removeFile(File file) {
        if (file == null || !file.exists()) {
            System.out.println("File is not existed!");
            return;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File subFile : files) {
                    if (subFile.isDirectory()) {
                        if (subFile.listFiles().length != 0) {
                            removeFile(subFile);
                        } else {
                            subFile.delete();
                            System.out.println(subFile.getAbsolutePath() + "is deleted!");
                        }
                    }
                }
            }
        } else {
            file.delete();
        }
    }

    public static List<String> findFiles(File file, List<String> list) {
        if (list == null) {
            list = new ArrayList<String>();
        }
        if (file == null || !file.exists()) {
            return null;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File subFile : files) {
                    if (subFile.isFile()) {
                        list.add(subFile.getAbsolutePath());
                    } else {
                        findFiles(subFile, list);
                    }
                }
            }
        } else {
            list.add(file.getAbsolutePath());
        }
        return list;
    }

    public static Long getSize(File file, long size) {
        if (file == null || !file.exists()) {
            return null;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File subFile : files) {
                    if (subFile.isFile()) {
                        size += subFile.length();
                    } else {
                        getSize(subFile, size);
                    }
                }
            }
        } else {
            size = file.length();
        }
        return size;
    }

    public static Map<String, Integer> countFile(File file, List<String> list, Map<String, Integer> map) {

        if (list == null) {
            list = new ArrayList<String>();
        }
        if (map == null) {
            map = new HashMap<String, Integer>();
        }
        if (file == null || !file.exists()) {
            return null;
        }
        List<String> files = FileUtil.findFiles(file, list);
        for (int i = 0; i < files.size(); i++) {
            String osuffix = files.get(i).substring(files.get(i).lastIndexOf(".") + 1, files.get(i).length());
            int cnt = 1;
            boolean flag = false;
            for (String s : files) {
                String isuffix = s.substring(s.lastIndexOf(".") + 1, s.length());
                if (Objects.equals(osuffix, isuffix)) {
                    cnt++;
                }
            }
            map.put(osuffix, cnt - 1);
        }
        return map;
    }

    public static List<String> findEmptyDir(File file, List<String> list) {
        if (list == null) {
            list = new ArrayList<String>();
        }
        if (file == null || !file.exists()) {
            return null;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File subFile : files) {
                    if (subFile.isFile()) {
                        continue;
                    } else if (subFile.isDirectory() && subFile.listFiles().length == 0) {
                        list.add(subFile.getAbsolutePath());
                    } else {
                        findEmptyDir(subFile, list);
                    }
                }
            }
        } else {
            System.out.println("It's a file,not a directory");
        }
        return list;
    }

    public static List<String> findSameFile(File file) {
        return null;
    }

    public static void viewFiles(File file) {
        //显示当前文件夹下内容
        File[] files = file.listFiles();
        for (File myFile : files) {
            System.out.println(myFile.toString());
        }
    }

    public static List<String> viewFilesRecursivly(File file, List<String> list) {
        File[] files = file.listFiles();
        for (File myFile : files) {
            if (myFile.isDirectory()) {
                viewFilesRecursivly(myFile, list);
            } else {
                list.add(myFile.getAbsolutePath());
            }
        }
        return list;
    }


    /**
     * nio写文件
     *
     * @param fileName
     * @param content
     * @return
     */
    public static void writeFile(String fileName, String content) {
        //获取通道
        FileOutputStream fos = null;
        FileChannel channel = null;
        ByteBuffer buffer = null;
        try {
            fos = new FileOutputStream(fileName);
            channel = fos.getChannel();
            buffer = ByteBuffer.wrap(content.getBytes());
            //将内容写到缓冲区
            fos.flush();
            channel.write(buffer);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (channel != null) {
                    channel.close();
                }
            } catch (IOException ee) {
                //log.error("IO异常", ee);
                ee.printStackTrace();
            }
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException ee) {
                ee.printStackTrace();
                //log.error("IO异常", ee);
            }
        }
    }

    /**
     * 读取文件
     *
     * @param fileName
     * @return
     */
    public static String readFile(String fileName) {
        String str = null;
        //获取通道
        FileInputStream fis = null;
        FileChannel channel = null;
        try {
            fis = new FileInputStream(fileName);
            channel = fis.getChannel();
            //指定缓冲区
            ByteBuffer buf = ByteBuffer.allocate(1024);
            //将通道中的数据读到缓冲区中
            channel.read(buf);
            byte[] arr = buf.array();
            str = new String(arr);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (channel != null) {
                    channel.close();
                }
            } catch (IOException ee) {
                ee.printStackTrace();
            }
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException ee) {
                ee.printStackTrace();
            }
        }
        return str.toString();
    }

    /**
     * 如果目录不存在，就创建文件
     *
     * @param dirPath
     * @return
     */
    public static String mkdirs(String dirPath) {
        try {
            File file = new File(dirPath);
            if (!file.exists()) {
                file.mkdirs();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dirPath;
    }

    /**
     * 判断文件是否存在
     *
     * @param filePath 文件路径
     * @return
     */
    public static boolean isExists(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }

    /**
     * 判断是否是文件夹
     *
     * @param path
     * @return
     */
    public static boolean isDir(String path) {
        File file = new File(path);
        if (file.exists()) {
            return file.isDirectory();
        } else {
            return false;
        }
    }


    /**
     * 删除空目录
     *
     * @param dir 将要删除的目录路径
     */
    private static void doDeleteEmptyDir(String dir) {
        boolean success = (new File(dir)).delete();
        if (success) {

        } else {

        }
    }

    /**
     * 递归删除目录下的所有文件及子目录下所有文件
     *
     * @param dir 将要删除的文件目录
     * @return boolean Returns "true" if all deletions were successful.
     * If a deletion fails, the method stops attempting to
     * delete and returns "false".
     */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            //递归删除目录中的子目录下
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        // 目录此时为空，可以删除
        return dir.delete();
    }

    /**
     * @param path
     * @param fileType
     * @Title: deleteFile
     * @Description: 除文件夹中指定的文件类型，如果有文件夹则不删除文件夹中的文件
     */
    public static void deleteFile(String path, String fileType) {
        if (StringUtils.isBlank(path)) {
            return;
        }
        File file = new File(path);
        if (file.isFile()) {//文件
            File[] listFiles = {file};
            deleteFileByType(listFiles, fileType);
        }
        if (file.isDirectory()) {//文件夹
            File[] listFiles = file.listFiles();
            deleteFileByType(listFiles, fileType);
        }
    }

    /**
     * @param path
     * @param fileType
     * @Title: cycleDeleteFile
     * @Description: 循环删除文件夹中指定的文件类型
     */
    public static void cycleDeleteFile(String path, String fileType) {
        if (StringUtils.isBlank(path)) {
            return;
        }
        File file = new File(path);
        if (file.isFile()) {//文件
            File[] listFiles = {file};
            deleteFileByType(listFiles, fileType);
        }
        if (file.isDirectory()) {//文件夹
            String[] files = file.list();
            for (String filePath : files) {
                cycleDeleteFile(file.getPath() + File.separator + filePath, fileType);
            }
        }
    }

    /**
     * @param listFiles
     * @param fileType
     * @Title: deleteFileByType
     * @Description: 判断文件是否是指定的类型，如果是则删除之
     */
    public static void deleteFileByType(File[] listFiles, String fileType) {
        if (listFiles == null || listFiles.length <= 0) {
            return;
        }
        String suffixName = "";
        for (File file : listFiles) {
            if (!file.isFile()) {//文件
                continue;
            }
            suffixName = getSuffixName(file);
            if (StringUtils.isBlank(suffixName)) {
                continue;
            }
            if (fileType.equals(suffixName)) {
                if (!file.delete()) {
                    //log.error("删除" + file.getPath() + "失败");
                }
            }
        }
    }

    /**
     * 删除文件，可以是单个文件或文件夹
     *
     * @param fileName 待删除的文件名
     * @return 文件删除成功返回true, 否则返回false
     */
    public static boolean delete(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("删除文件失败：" + fileName + "文件不存在");
            return false;
        } else {
            if (file.isFile()) {

                return deleteFile(fileName);
            } else {
                return deleteDirectory(fileName);
            }
        }
    }

    /**
     * 删除单个文件
     *
     * @param fileName 被删除文件的文件名
     * @return 单个文件删除成功返回true, 否则返回false
     */
    public static boolean deleteFile(String fileName) {
        File file = new File(fileName);
        if (file.isFile() && file.exists()) {
            file.delete();
            System.out.println("删除单个文件" + fileName + "成功！");
            return true;
        } else {
            System.out.println("删除单个文件" + fileName + "失败！");
            return false;
        }
    }

    /**
     * 删除目录（文件夹）以及目录下的文件
     *
     * @param dir 被删除目录的文件路径
     * @return 目录删除成功返回true, 否则返回false
     */
    public static boolean deleteDirectory(String dir) {
        // 如果dir不以文件分隔符结尾，自动添加文件分隔符
        if (!dir.endsWith(File.separator)) {
            dir = dir + File.separator;
        }
        File dirFile = new File(dir);
        // 如果dir对应的文件不存在，或者不是一个目录，则退出
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            System.out.println("删除目录失败" + dir + "目录不存在！");
            return false;
        }
        boolean flag = true;
        // 删除文件夹下的所有文件(包括子目录)
        File[] files = dirFile.listFiles();
        for (int i = 0; i < files.length; i++) {
            // 删除子文件
            if (files[i].isFile()) {
                flag = deleteFile(files[i].getAbsolutePath());
                if (!flag) {
                    break;
                }
            }
            // 删除子目录
            else {
                flag = deleteDirectory(files[i].getAbsolutePath()); // 如果目录下还有目录，这反复调用
                // deleteDirectory
                if (!flag) {
                    break;
                }
            }
        }
        if (!flag) {
            System.out.println("删除目录失败");
            return false;
        }
        // 删除当前目录
        if (dirFile.delete()) {
            System.out.println("删除目录" + dir + "成功！");
            return true;
        } else {
            System.out.println("删除目录" + dir + "失败！");
            return false;
        }
    }

    /**
     * @param file
     * @return
     * @Title: getSuffixName
     * @Description: 返回文件的后缀名, 如JPEG：FFD8FF
     */
    public static String getSuffixName(File file) {
        if (!file.isFile()) {
            return null;
        }
        FileInputStream is = null;
        byte[] src = new byte[3];
        StringBuilder sBuilder = null;
        String hv = null;
        try {
            is = new FileInputStream(file);
            src = new byte[3];
            is.read(src, 0, src.length);
            is.close();
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        if (src == null || src.length <= 0) {
            return null;
        }
        sBuilder = new StringBuilder();
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                sBuilder.append(0);
            }
            sBuilder.append(hv);
        }
        if (sBuilder.length() <= 0) {
            return null;
        }
        return sBuilder.toString().toUpperCase();
    }


    /**
     * File 文件或者目录重命名
     *
     * @param oldFilePath 旧文件路径
     * @param newName     新的文件名,可以是单个文件名和绝对路径
     * @return
     */
    public static boolean renameTo(String oldFilePath, String newName) {
        try {
            File oldFile = new File(oldFilePath);
            //若文件存在
            if (oldFile.exists()) {
                //判断是全路径还是文件名
                if (newName.indexOf("/") < 0 && newName.indexOf("\\") < 0) {
                    //单文件名，判断是windows还是Linux系统
                    String absolutePath = oldFile.getAbsolutePath();
                    if (newName.indexOf("/") > 0) {
                        //Linux系统
                        newName = absolutePath.substring(0, absolutePath.lastIndexOf("/") + 1) + newName;
                    } else {
                        newName = absolutePath.substring(0, absolutePath.lastIndexOf("\\") + 1) + newName;
                    }
                }
                File file = new File(newName);
                //判断重命名后的文件是否存在
                if (file.exists()) {
                    System.out.println("该文件已存在,不能重命名");
                } else {
                    //不存在，重命名
                    return oldFile.renameTo(file);
                }
            } else {
                System.out.println("原该文件不存在,不能重命名");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * 文件复制
     *
     * @param src 要复制
     * @param des 复制到
     * @return
     */
    public static boolean copyDir(String src, String des) {
        try {
            //初始化文件复制
            File file1 = new File(src);
            if (!file1.exists()) {
                return false;
            } else {
                //把文件里面内容放进数组
                File[] fs = file1.listFiles();
                //初始化文件粘贴
                File file2 = new File(des);
                //判断是否有这个文件有不管没有创建
                if (!file2.exists()) {
                    file2.mkdirs();
                }
                //遍历文件及文件夹
                for (File f : fs) {
                    if (f.isFile()) {
                        //文件
                        fileCopy(f.getPath(), des + File.separator + f.getName()); //调用文件拷贝的方法
                    } else if (f.isDirectory()) {
                        //文件夹
                        copyDir(f.getPath(), des + File.separator + f.getName());//继续调用复制方法      递归的地方,自己调用自己的方法,就可以复制文件夹的文件夹了
                    }
                }
                return true;
            }
        } catch (Exception e) {
            e.getStackTrace();
            return false;
        }
    }

    /**
     * 文件拷贝操作
     *
     * @param sourceFile 主
     * @param targetFile 到
     */
    public static void copy(String sourceFile, String targetFile) {
        File source = new File(sourceFile);
        File target = new File(targetFile);
        target.getParentFile().mkdirs();
        FileInputStream fis = null;
        FileOutputStream fos = null;
        FileChannel in = null;
        FileChannel out = null;
        try {
            fis = new FileInputStream(source);
            fos = new FileOutputStream(target);
            in = fis.getChannel();//得到对应的文件通道
            out = fos.getChannel();//得到对应的文件通道
            in.transferTo(0, in.size(), out);//连接两个通道，并且从in通道读取，然后写入out通道
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
                if (fos != null) {
                    fos.close();
                }
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 通过上一层目录和目录名得到最后的目录层次
     *
     * @param previousDir 上一层目录
     * @param dirName     当前目录名
     * @return
     */
    public static String getSaveDir(String previousDir, String dirName) {
        if (StringUtils.isNotBlank(previousDir)) {
            dirName = previousDir + "/" + dirName + "/";
        } else {
            dirName = dirName + "/";
        }
        return dirName;
    }


    //其他方法
    public static Set<String> getDirName(String fName) {
        Set<String> set = new HashSet<>();
        String fileName = null;
        File tempFile = new File(fName.trim());
        if (tempFile.exists()) {
            File[] array = tempFile.listFiles();
            for (int i = 0; i < array.length; i++) {
                set.add(array[i].getName().toString());
            }
        }
        System.out.println(set);
        return set;
    }

    /**
     * 文件复制的具体方法
     */
    private static void fileCopy(String src, String des) throws Exception {
        //io流固定格式
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(src));
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(des));
        int i = -1;//记录获取长度
        byte[] bt = new byte[2014];//缓冲区
        while ((i = bis.read(bt)) != -1) {
            bos.write(bt, 0, i);
        }
        bis.close();
        bos.close();
        //关闭流
    }

    public static String getFileName(String fName) {
        String fileName = null;
        File tempFile = new File(fName.trim());
        if (tempFile.exists()) {
            File[] array = tempFile.listFiles();
            for (int i = 0; i < array.length; i++) {
                String houzhui = array[i].toString().substring(array[i].toString().lastIndexOf(".") + 1);
                if (houzhui.equals("iw") || houzhui.equals(".zip")) {
                    fileName = array[i].getName();
                }
            }
        }
        return fileName;
    }


    /**
     * 网络
     *
     * @param path
     * @param response
     * @throws IOException
     */
    public static void downloadDoc(String path, HttpServletResponse response) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            throw new RuntimeException("文件不存在");
        }
        downloadDoc(file, response);
    }


    /**
     * NIO download
     *
     * @param file: 需要进行下载文件
     * @description:
     * @return: void
     */
    public static void downloadDoc(File file, HttpServletResponse response) throws IOException {
        OutputStream outputStream = response.getOutputStream();
        String contentType = Files.probeContentType(Paths.get(file.getAbsolutePath()));
        //设置响应头
        response.setHeader("Content-Type", contentType);
        response.setHeader("Content-Disposition", "attachment;filename=" + new String(file.getName().getBytes("utf-8"), "ISO8859-1"));
        response.setContentLength((int) file.length());
        //获取文件输入流
        FileInputStream fileInputStream = new FileInputStream(file);
        //获取输出流通道
        WritableByteChannel writableByteChannel = Channels.newChannel(outputStream);
        FileChannel fileChannel = fileInputStream.getChannel();
        //采用零拷贝的方式实现文件的下载
        fileChannel.transferTo(0, fileChannel.size(), writableByteChannel);
        //关闭对应的资源
        fileChannel.close();
        outputStream.flush();
        writableByteChannel.close();
    }


    /**
     * 网络 NIO upload
     *
     * @param file
     * @return
     */
    public AjaxResult uploading(MultipartFile file) {
        //获取文件名
        String realName = file.getOriginalFilename();
        String newName = null;
        if (realName != null && realName != "") {
            //获取文件后缀
            String suffixName = realName.substring(realName.lastIndexOf("."));
            //生成新名字
            newName = UUID.randomUUID().toString().replaceAll("-", "") + suffixName;
        } else {
            return AjaxResult.error("文件名不可为空");
        }
        //创建流
        FileInputStream fis = null;
        FileOutputStream fos = null;
        //创建通道
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        try {
            fis = (FileInputStream) file.getInputStream();
            //开始上传
            fos = new FileOutputStream(new File(uploadDir + File.separator + newName));
            //通道间传输
            inChannel = fis.getChannel();
            outChannel = fos.getChannel();
            //上传
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            return AjaxResult.error("文件上传路径错误");
        } finally {
            //关闭资源
            try {
                if (fis != null) {
                    fis.close();
                }
                if (fos != null) {
                    fos.close();
                }
                if (inChannel != null) {
                    inChannel.close();
                }
                if (outChannel != null) {
                    outChannel.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return AjaxResult.success(newName);
    }


    public static String getPrintSize(long size) {
        long b = size / 1 % 1024; //b
        long kb = size / 1024 % 1024;//kb
        long mb = size / (1024 * 1024) % 1024;//mb
        long gb = size / (1024 * 1024 * 1024) % 1024;//gb
        String gbStr = "";
        String mbStr = "";
        String kbStr = "";
        String bStr = "";
        if (gb > 0) {
            gbStr += gb + "GB";
        }
        if (mb > 0) {
            mbStr += mb + "MB";
        }
        if (kb > 0) {
            kbStr += kb + "kb";
        }
        if (b > 0) {
            bStr += b + "b";
        }
        return gbStr + mbStr + kbStr + bStr;
    }


    public static String calculateMD5(String filePath) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            try (InputStream fis = new FileInputStream(filePath)) {
                byte[] byteArray = new byte[1024];
                int bytesCount = 0;

                while ((bytesCount = fis.read(byteArray)) != -1) {
                    digest.update(byteArray, 0, bytesCount);
                }
            }
            byte[] bytes = digest.digest();
            String hashString = new BigInteger(1, bytes).toString(16);
            log.info("md5:{}", hashString);
            return hashString;
        } catch (Exception e) {
            throw new RuntimeException("Unable to calculate MD5 for file: " + filePath, e);
        }
    }

    public String getMd5(MultipartFile file) {
        try {
            byte[] uploadBytes = file.getBytes();
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest(uploadBytes);
            String hashString = new BigInteger(1, digest).toString(16);
            return hashString;
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
        return null;

    }


}
