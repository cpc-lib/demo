package cc.ivera.service;


import cc.ivera.model.vo.FileUploadInfo;
import cc.ivera.utils.R;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

public interface UploadService {
    /**
     * 分片上传初始化
     *
     * @param fileUploadInfo
     * @return Map<String, Object>
     */
    Map<String, Object> initMultiPartUpload(FileUploadInfo fileUploadInfo);

    /**
     * 完成分片上传
     *
     * @param  fileUploadInfo
     * @return String
     */
    String mergeMultipartUpload(FileUploadInfo fileUploadInfo);

    /**
     *  通过 md5 获取已上传的数据
     * @param md5 String
     * @return Mono<Map<String, Object>>
     */
    R getByFileMD5(String md5);

    /**
     *  获取文件地址
     * @param bucketName
     * @param fileName
     *
     */
    String getFliePath(String bucketName, String fileName);


    /**
     * 单文件上传
     * @param file
     * @param bucketName
     * @return
     */
    String upload(MultipartFile file, String bucketName);

}