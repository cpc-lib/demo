package cc.ivera.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.csource.fastdfs.ProtoCommon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import cc.ivera.common.api.CommonResult;
import cc.ivera.dto.FastDFSUploadDto;
import cc.ivera.util.FastDFSClient;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;

@Api(tags = "FastDFSController", description = "FastDFS对象存储管理")
@Controller
@RequestMapping("/fastdfs")
public class FastDFSController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FastDFSController.class);


    @Autowired
    private FastDFSClient client;

    @Value("${fdfs.web-server-url}")
    private String fastdfsUrl;

    @Value("${fdfs.http.secret_key}")
    private String fastdfsToken;


    @ApiOperation("文件上传")
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult upload(@RequestParam("file") MultipartFile file) {
        try {
            String url = client.uploadFile(file);
            FastDFSUploadDto dto = new FastDFSUploadDto();
            dto.setName(file.getOriginalFilename());
            dto.setUrl(url);
            return CommonResult.success(dto);
        } catch (Exception e) {
            LOGGER.info("上传发生错误: {}！", e.getMessage());
        }
        return CommonResult.failed();
    }

    @ApiOperation("文件删除")
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult delete(@RequestParam("fileUrl") String objectName) {
        try {
            client.deleteFile(objectName);
            return CommonResult.success(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return CommonResult.failed();
    }

    /**
     * 文件下载
     *
     * @param fid
     * @param response
     * @throws Exception
     */
    @ApiOperation("文件下载")
    @RequestMapping(value = "/download", method = RequestMethod.POST)
    public void download(HttpServletResponse response, @RequestParam("fileUrl") String fileUrl) throws Exception {
        int i = fileUrl.lastIndexOf("/");
        String fileName = fileUrl.substring(i + 1);
        byte[] data = client.download(fileUrl);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));
        ServletOutputStream outputStream = response.getOutputStream();
        response.getOutputStream().write(data);
        response.getOutputStream().flush();
        //IOUtils.write(data, outputStream);
    }

    /**
     * 获取访问服务器的token，拼接到地址后面
     *
     * @param fid        文件路径 group1/M00/00/00/wKgzgFnkTPyAIAUGAAEoRmXZPp876.jpeg
     * @param secret_key 密钥
     * @return 返回token，如： token=078d370098b03e9020b82c829c205e1f&ts=1508141521
     */
    public static String getToken(String fid, String secret_key, String IP) {
        String substring = fid.substring(fid.indexOf("/") + 1);
        //unix时间戳 以秒为单位
        int ts = (int) (System.currentTimeMillis() / 1000);
        String token = new String();
        try {
            token = ProtoCommon.getToken(substring, ts, secret_key);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        StringBuilder sb = new StringBuilder();
        sb.append(IP);
        sb.append(fid);
        sb.append("?token=").append(token);
        sb.append("&ts=").append(ts);
        return sb.toString();
    }
}
