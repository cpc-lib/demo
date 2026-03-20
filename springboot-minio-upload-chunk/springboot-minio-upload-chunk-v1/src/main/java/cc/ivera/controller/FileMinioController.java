package cc.ivera.controller;


import cc.ivera.model.vo.FileUploadInfo;
import cc.ivera.service.UploadService;
import cc.ivera.utils.MinioUtils;
import cc.ivera.utils.R;
import cc.ivera.utils.RedisUtil;
import cc.ivera.utils.RespEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.Resource;

/**
 * minio上传流程
 *
 * 1.检查数据库中是否存在上传文件
 *
 * 2.根据文件信息初始化，获取分片预签名url地址，前端根据url地址上传文件
 *
 * 3.上传完成后，将分片上传的文件进行合并
 *
 * 4.保存文件信息到数据库
 */
@RestController
@Slf4j
@RequestMapping("upload")
public class FileMinioController {

    @Resource
    private UploadService uploadService;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private MinioUtils minioUtils;

    /**
     * @description 获取上传文件
     * @param fileMD5 文件md5
     * @return {@link R }
     * @author LGY
     * @date 2023/04/26 16:00
     */
    @GetMapping("/getUploadingFile/{fileMD5}")
    public R getUploadingFile(@PathVariable String fileMD5) {
        if (StringUtils.isBlank(fileMD5)) {
            return R.error();
        }
        FileUploadInfo fileUploadInfo = (FileUploadInfo) redisUtil.get(fileMD5);
        if (fileUploadInfo != null) {
            // 查询上传后的分片数据
            fileUploadInfo.setChunkUploadedList(minioUtils.getChunkByFileMD5(fileUploadInfo.getFileName(), fileUploadInfo.getUploadId(), fileUploadInfo.getFileType()));
            return R.ok().setData(fileUploadInfo);
        }
        return R.error();
    }

    /**
     * 校验文件是否存在
     *
     * @param md5 String
     * @return ResponseResult<Object>
     */
    @GetMapping("/multipart/check")
    public R checkFileUploadedByMd5(@RequestParam("md5") String md5) {
        log.info("REST: 通过查询 <{}> 文件是否存在、是否进行断点续传", md5);
        if (StringUtils.isEmpty(md5)) {
            log.error("查询文件是否存在、入参无效");
            return R.error(RespEnum.ACCESS_PARAMETER_INVALID);
        }
        return uploadService.getByFileMD5(md5);
    }

    /**
     * 分片初始化
     *
     * @param fileUploadInfo 文件信息
     * @return ResponseResult<Object>
     */
    @PostMapping("/multipart/init")
    public R initMultiPartUpload(@RequestBody FileUploadInfo fileUploadInfo) {
        log.info("REST: 通过 <{}> 初始化上传任务", fileUploadInfo);
        return R.ok().setData(uploadService.initMultiPartUpload(fileUploadInfo));
    }

    /**
     * 完成上传
     *
     * @param fileUploadInfo  文件信息
     * @return ResponseResult<Object>
     */
    @PostMapping("/multipart/merge")
    public R completeMultiPartUpload(@RequestBody FileUploadInfo fileUploadInfo) {
        log.info("REST: 通过 {} 合并上传任务", fileUploadInfo);
        //合并文件
        String url = uploadService.mergeMultipartUpload(fileUploadInfo);
        //获取上传文件地址
        if (StringUtils.isNotBlank(url)) {
            return R.ok().setData(url);
        }
        return R.error();
    }

    @PostMapping("/multipart/uploadScreenshot")
    public R uploaduploadScreenshot(@RequestPart("photos") MultipartFile[] photos,
                                    @RequestParam("buckName") String buckName) {
        log.info("REST: 上传文件信息 <{}> ", photos);

        for (MultipartFile photo : photos) {
            if (!photo.isEmpty()) {
                uploadService.upload(photo, buckName);
            }
        }
        return R.ok();
    }


    @RequestMapping("/createBucket")
    public void createBucket(@RequestParam("bucketName") String bucketName) {
        String bucket = minioUtils.createBucket(bucketName);
    }

}
