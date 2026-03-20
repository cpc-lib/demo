package cc.ivera.controller;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sun.misc.BASE64Decoder;
import cc.ivera.entity.FileRecord;
import cc.ivera.mapper.FileRecordMapper;
import cc.ivera.util.BASE64DecodedMultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static java.io.File.separator;

@Controller
@CrossOrigin
public class FileController {

    @Value("${upload.dir}")
    private String uploadDir;

    @Autowired
    private FileRecordMapper fileRecordMapper;

    @PostMapping("/upload")
    @ResponseBody
    public String uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        // 生成文件ID
        String fileId = generateFileId();

        // 将文件保存到上传目录
        String filePath = uploadDir + separator + fileId;


        file.transferTo(new File(filePath));

        // 保存文件记录到数据库
        FileRecord fileRecord = new FileRecord();
        String originalFilename = file.getOriginalFilename();
        System.out.println(originalFilename);

        fileRecord.setFileName(originalFilename);
        fileRecord.setFileId(fileId);
        fileRecordMapper.insertFileRecord(fileRecord);

        return fileId;
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable("fileId") String fileId) throws IOException {
        // 根据文件ID从数据库获取文件记录
        FileRecord fileRecord = fileRecordMapper.findByFileId(fileId);
        if (fileRecord == null) {
            return ResponseEntity.notFound().build();
        }

        String filePath = uploadDir + separator + fileId;
        File file = new File(filePath);

        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        // 创建文件输入流
        InputStream inputStream = new FileInputStream(file);

        // 设置响应头，告诉浏览器文件的类型和下载方式
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileRecord.getFileName());

        // 创建包含文件流的 ResponseEntity
        return ResponseEntity.status(HttpStatus.OK).headers(headers).body(new InputStreamResource(inputStream));
    }


    @GetMapping("/filedownload")
    public void downloadFile(@RequestParam("fileId") String fileId, HttpServletResponse response) throws IOException {
        // 根据文件ID从数据库获取文件记录
        FileRecord fileRecord = fileRecordMapper.findByFileId(fileId);
        if (fileRecord == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String filePath = uploadDir + separator + fileId;
        File file = new File(filePath);

        if (!file.exists()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 创建文件输入流
        InputStream inputStream = new FileInputStream(file);

        // 设置响应头，告诉浏览器文件的类型和下载方式
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileRecord.getFileName());

        // 获取输出流
        OutputStream outputStream = response.getOutputStream();

        // 将文件流写入输出流
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        // 关闭流
        inputStream.close();
        outputStream.close();
    }


    @GetMapping("/file/base64")
    @ResponseBody
    public String getBase64(@RequestParam("fileId") String fileId, HttpServletResponse response) throws IOException {
        FileRecord fileRecord = fileRecordMapper.findByFileId(fileId);
        String fileName = fileRecord.getFileName();
        String extension = FilenameUtils.getExtension(fileName);
        if ("png".equals(extension) || "jpg".equals(extension) || "jpeg".equals(extension)) {
            String filePath = uploadDir + separator + fileId;
            File file = new File(filePath);
            if (!file.exists()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return null;
            } else {
                byte[] bytes = FileUtils.readFileToByteArray(file);
                String base64 = new String(Base64.getEncoder().encode(bytes), StandardCharsets.UTF_8);
                return base64;
            }
        } else {
            return null;
        }
    }


    @PostMapping("/base64/file")
    @ResponseBody
    public String convertBase64ToFile(@RequestParam("base64") String base64) throws IOException {
        String base64encodedString = Base64.getEncoder().encodeToString(base64.getBytes("utf-8"));
        byte[] decode = Base64.getDecoder().decode(base64encodedString);
        String fileId = generateFileId();
        String fileName = fileId + ".png";
        String filePath = uploadDir + separator + fileId;
        File file = new File(filePath);
        //写入到固定位置
        FileUtils.writeByteArrayToFile(file, decode);
        FileRecord fileRecord = new FileRecord();
        String originalFilename = fileName;
        fileRecord.setFileName(originalFilename);
        fileRecord.setFileId(fileId);
        fileRecordMapper.insertFileRecord(fileRecord);
        return fileId;
    }


    @RequestMapping(value = "/uploadbase64", method = RequestMethod.POST)
    @ResponseBody
    public String uploadbase64(@RequestParam("base64") String base64) throws IOException {
        MultipartFile file = BASE64DecodedMultipartFile.base64ToMultipart(base64);
        // 获取文件名
        String fileName = file.getOriginalFilename();
        String fileId = generateFileId();
        String filePath = uploadDir + separator + fileId;
        file.transferTo(new File(filePath));
        FileRecord fileRecord = new FileRecord();
        String originalFilename = fileName;
        fileRecord.setFileName(originalFilename);
        fileRecord.setFileId(fileId);
        fileRecordMapper.insertFileRecord(fileRecord);
        return fileId;
    }


    @RequestMapping(value = "/uploadbase", method = RequestMethod.POST)
    @ResponseBody
    public String upload(@RequestParam("base64") String base64) throws IOException {
        String fileId = generateFileId();
        MultipartFile file = convert(base64, fileId, "png");
        // 获取文件名
        String fileName = file.getOriginalFilename();
        String filePath = uploadDir + separator + fileId;
        file.transferTo(new File(filePath));
        FileRecord fileRecord = new FileRecord();
        String originalFilename = fileName;
        fileRecord.setFileName(originalFilename);
        fileRecord.setFileId(fileId);
        fileRecordMapper.insertFileRecord(fileRecord);
        return fileId;
    }


    private String generateFileId() {
        return UUID.randomUUID().toString();
    }


    public static MultipartFile base64ToMultipart(String base64) {
        try {
            String[] baseStrs = base64.split(",");

            BASE64Decoder decoder = new BASE64Decoder();
            byte[] b = new byte[0];
            b = decoder.decodeBuffer(baseStrs[1]);

            for (int i = 0; i < b.length; ++i) {
                if (b[i] < 0) {
                    b[i] += 256;
                }
            }
            return new BASE64DecodedMultipartFile(b, baseStrs[0]);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    public static MultipartFile convert(String base64, String fileId, String suffix) throws IOException {
        // 解码Base64字符串为字节数组
        String base64encodedString = Base64.getEncoder().encodeToString(base64.getBytes("utf-8"));
        byte[] decodedBytes = Base64.getDecoder().decode(base64encodedString);

        // 创建临时文件
        File tempFile = File.createTempFile(fileId + "." + suffix, null);

        // 将字节数组写入临时文件
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(decodedBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 使用Spring的MockMultipartFile创建MultipartFile对象
        return new MockMultipartFile(tempFile.getName(), tempFile.getName(), null, tempFile.toURI().toURL().openStream());
    }
}
