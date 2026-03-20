package cc.ivera.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cc.ivera.mapper.FilesMapper;
import cc.ivera.model.po.Files;
import cc.ivera.model.vo.FileUploadInfo;
import cc.ivera.service.UploadService;
import cc.ivera.utils.MinioUtils;
import cc.ivera.utils.R;
import cc.ivera.utils.RedisUtil;
import cc.ivera.utils.RespEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class UploadServiceImpl implements UploadService {

    @Resource
    private FilesMapper filesMapper;

    @Resource
    private MinioUtils minioUtils;

    @Resource
    private RedisUtil redisUtil;

    @Value("${minio.breakpoint-time}")
    private Integer breakpointTime;

    /**
     * 通过 md5 获取已上传的数据（断点续传）
     *
     * @param md5 String
     * @return Mono<Map < String, Object>>
     */
    @Override
    public R getByFileMD5(String md5) {

        log.info("tip message: 通过 <{}> 查询redis是否存在", md5);
        // 从redis获取文件名称和id
        FileUploadInfo fileUploadInfo = (FileUploadInfo) redisUtil.get(md5);
        if (fileUploadInfo != null) {
            // 正在上传，查询上传后的分片数据
            List<Integer> chunkList = minioUtils.getChunkByFileMD5(fileUploadInfo.getFileName(), fileUploadInfo.getUploadId(), fileUploadInfo.getFileType());
            fileUploadInfo.setChunkUploadedList(chunkList);
            return R.ok(RespEnum.UPLOADING).setData(fileUploadInfo);

        }

        log.info("tip message: 通过 <{}> 查询mysql是否存在", md5);
        // 查询数据库是否上传成功
        Files one = filesMapper.selectOne(new LambdaQueryWrapper<Files>().eq(Files::getFileMd5, md5));
        if (one != null) {
            FileUploadInfo mysqlsFileUploadInfo = new FileUploadInfo();
            BeanUtils.copyProperties(one, mysqlsFileUploadInfo);
            return R.ok(RespEnum.UPLOADSUCCESSFUL).setData(mysqlsFileUploadInfo);
        }

        return R.ok(RespEnum.NOT_UPLOADED);
    }


    /**
     * 文件分片上传
     *
     * @param fileUploadInfo
     * @return Mono<Map < String, Object>>
     */
    @Override
    public Map<String, Object> initMultiPartUpload(FileUploadInfo fileUploadInfo) {

        FileUploadInfo redisFileUploadInfo = (FileUploadInfo) redisUtil.get(fileUploadInfo.getFileMd5());
        if (redisFileUploadInfo != null) {
            fileUploadInfo = redisFileUploadInfo;
        }
        log.info("tip message: 通过 <{}> 开始初始化<分片上传>任务", fileUploadInfo);
        // 获取桶
        String bucketName = minioUtils.getBucketName(fileUploadInfo.getFileType());

        // 单文件上传
        if (fileUploadInfo.getChunkNum() == 1) {
            log.info("tip message: 当前分片数量 <{}> 进行单文件上传", fileUploadInfo.getChunkNum());
            Files files = saveFileToDB(fileUploadInfo);
            String fileName = files.getUrl().substring(files.getUrl().lastIndexOf("/") + 1);
            return minioUtils.getUploadObjectUrl(fileName, bucketName);
        }
        // 分片上传
        else {
            log.info("tip message: 当前分片数量 <{}> 进行分片上传", fileUploadInfo.getChunkNum());
            Map<String, Object> map = minioUtils.initMultiPartUpload(fileUploadInfo, fileUploadInfo.getFileName(), fileUploadInfo.getChunkNum(), fileUploadInfo.getContentType(), bucketName);
            String uploadId = (String) map.get("uploadId");
            fileUploadInfo.setUploadId(uploadId);
            redisUtil.set(fileUploadInfo.getFileMd5(),fileUploadInfo,breakpointTime*60*60*24);
            return map;
        }
    }


    /**
     * 文件合并
     *
     * @param
     * @return String
     */
    @Override
    public String mergeMultipartUpload(FileUploadInfo fileUploadInfo) {
        log.info("tip message: 通过 <{}> 开始合并<分片上传>任务", fileUploadInfo);
        FileUploadInfo redisFileUploadInfo = (FileUploadInfo) redisUtil.get(fileUploadInfo.getFileMd5());
        if(redisFileUploadInfo!=null){
            fileUploadInfo.setFileName(redisFileUploadInfo.getFileName());
        }
        boolean result = minioUtils.mergeMultipartUpload(fileUploadInfo.getFileName(), fileUploadInfo.getUploadId(), fileUploadInfo.getFileType());

        //合并成功
        if (result) {
            //存入数据库
            Files files = saveFileToDB(fileUploadInfo);
            redisUtil.del(fileUploadInfo.getFileMd5());
            return files.getUrl();
        }
        return null;
    }

    @Override
    public String getFliePath(String bucketName, String fileName) {
        return minioUtils.getFliePath(bucketName, fileName);
    }

    @Override
    public String upload(MultipartFile file, String bucketName) {
        minioUtils.upload(file, bucketName);
        return getFliePath(bucketName, file.getName());
    }

    private Files saveFileToDB(FileUploadInfo fileUploadInfo) {
        String suffix = fileUploadInfo.getFileName().substring(fileUploadInfo.getFileName().lastIndexOf("."));
        String url = this.getFliePath(fileUploadInfo.getFileType().toLowerCase(), fileUploadInfo.getFileMd5() + suffix);
        //存入数据库
        Files files = new Files();
        BeanUtils.copyProperties(fileUploadInfo, files);
        files.setBucketName(fileUploadInfo.getFileType());
        files.setUrl(url);
        filesMapper.insert(files);
        return files;
    }
}