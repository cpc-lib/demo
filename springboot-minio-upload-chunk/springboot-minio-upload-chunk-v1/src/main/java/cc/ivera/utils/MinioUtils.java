package cc.ivera.utils;


import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.HashMultimap;
import cc.ivera.config.CustomMinioClient;
import cc.ivera.model.vo.FileUploadInfo;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Part;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Slf4j
@Component
public class MinioUtils {

    @Value(value = "${minio.endpoint}")
    private String endpoint;
    @Value(value = "${minio.accesskey}")
    private String accesskey;
    @Value(value = "${minio.secretkey}")
    private String secretkey;

    @Value(value = "${minio.expiry}")
    private Integer expiry;

    private CustomMinioClient customMinioClient;

    /**
     * 用spring的自动注入会注入失败
     */
    @PostConstruct
    public void init() {
        MinioClient minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accesskey, secretkey)
                .build();
        customMinioClient = new CustomMinioClient(minioClient);
    }


    /**
     * 单文件签名上传
     *
     * @param objectName 文件全路径名称
     * @param bucketName 桶名称
     * @return /
     */
    public Map<String, Object> getUploadObjectUrl(String objectName, String bucketName) {
        try {
            log.info("tip message: 通过 <{}-{}> 开始单文件上传<minio>", objectName, bucketName);
            Map<String, Object> resMap = new HashMap();
            List<String> partList = new ArrayList<>();
            String url = customMinioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(expiry, TimeUnit.DAYS)
                            .build());
            log.info("tip message: 单个文件上传、成功");
            partList.add(url);
            resMap.put("uploadId", "SingleFileUpload");
            resMap.put("urlList", partList);
            return resMap;
        } catch (Exception e) {
            log.error("error message: 单个文件上传失败、原因:", e);
            // 返回 文件上传失败
            return null;
        }
    }

    /**
     * 初始化分片上传
     *
     * @param fileUploadInfo
     * @param objectName     文件全路径名称
     * @param chunkNum       分片数量
     * @param contentType    类型，如果类型使用默认流会导致无法预览
     * @param bucketName     桶名称
     * @return Mono<Map < String, Object>>
     */
    public Map<String, Object> initMultiPartUpload(FileUploadInfo fileUploadInfo, String objectName, int chunkNum, String contentType, String bucketName) {
        log.info("tip message: 通过 <{}-{}-{}-{}> 开始初始化<分片上传>数据", objectName, chunkNum, contentType, bucketName);
        Map<String, Object> resMap = new HashMap<>();
        try {
            if (CharSequenceUtil.isBlank(contentType)) {
                contentType = "application/octet-stream";
            }
            HashMultimap<String, String> headers = HashMultimap.create();

            headers.put("Content-Type", contentType);

            //获取uploadId
            String uploadId = null;
            if (StringUtils.isBlank(fileUploadInfo.getUploadId())) {
                uploadId = customMinioClient.initMultiPartUpload(bucketName, null, objectName, headers, null);
            } else {
                uploadId = fileUploadInfo.getUploadId();
            }

            resMap.put("uploadId", uploadId);

            fileUploadInfo.setUploadId(uploadId);
            fileUploadInfo.setChunkNum(chunkNum);

            List<String> partList = new ArrayList<>();

            Map<String, String> reqParams = new HashMap<>();
            reqParams.put("uploadId", uploadId);
            for (int i = 1; i <= chunkNum; i++) {
                reqParams.put("partNumber", String.valueOf(i));
                String uploadUrl = customMinioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.PUT)
                                .bucket(bucketName)
                                .object(objectName)
                                .expiry(1, TimeUnit.DAYS)
                                .extraQueryParams(reqParams)
                                .build());
                partList.add(uploadUrl);
            }
            log.info("tip message: 文件初始化<分片上传>、成功");
            resMap.put("urlList", partList);
            return resMap;
        } catch (Exception e) {
            log.error("error message: 初始化分片上传失败、原因:", e);
            // 返回 文件上传失败
            return R.error(RespEnum.UPLOAD_FILE_FAILED);
        }
    }

    /**
     * 分片上传完后合并
     *
     * @param objectName 文件全路径名称
     * @param uploadId   返回的uploadId
     * @param bucketName 桶名称
     * @return boolean
     */
    public boolean mergeMultipartUpload(String objectName, String uploadId, String bucketName) {
        try {
            log.info("tip message: 通过 <{}-{}-{}> 合并<分片上传>数据", objectName, uploadId, bucketName);
            //目前仅做了最大1000分片
            Part[] parts = new Part[1000];
            // 查询上传后的分片数据
            ListPartsResponse partResult = customMinioClient.listMultipart(bucketName, null, objectName, 1000, 0, uploadId, null, null);
            int partNumber = 1;
            for (Part part : partResult.result().partList()) {
                parts[partNumber - 1] = new Part(partNumber, part.etag());
                partNumber++;
            }
            // 合并分片
            customMinioClient.mergeMultipartUpload(bucketName, null, objectName, uploadId, parts, null, null);

        } catch (Exception e) {
            log.error("error message: 合并失败、原因:", e);
            //TODO删除redis的数据
            return false;
        }
        return true;
    }

    /**
     * 通过 sha256 获取上传中的分片信息
     *
     * @param objectName 文件全路径名称
     * @param uploadId   返回的uploadId
     * @param bucketName 桶名称
     * @return Mono<Map < String, Object>>
     */
    public List<Integer> getChunkByFileMD5(String objectName, String uploadId, String bucketName) {
        log.info("通过 <{}-{}-{}> 查询<minio>上传分片数据", objectName, uploadId, bucketName);
        try {
            // 查询上传后的分片数据
            ListPartsResponse partResult = customMinioClient.listMultipart(bucketName, null, objectName, 1000, 0, uploadId, null, null);
            return partResult.result().partList().stream().map(Part::partNumber).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("error message: 查询上传后的分片信息失败、原因:", e);
            return null;
        }
    }

    /**
     * 获取文件下载地址
     *
     * @param bucketName 桶名称
     * @param fileName   文件名
     * @return
     */
    public String getFliePath(String bucketName, String fileName) {
        return StrUtil.format("{}/{}/{}", endpoint, bucketName, fileName);//文件访问路径
    }

    /**
     * 创建一个桶
     *
     * @return
     */
    public String createBucket(String bucketName) {
        try {
            BucketExistsArgs bucketExistsArgs = BucketExistsArgs.builder().bucket(bucketName).build();
            //如果桶存在
            if (customMinioClient.bucketExists(bucketExistsArgs)) {
                return bucketName;
            }
            MakeBucketArgs makeBucketArgs = MakeBucketArgs.builder().bucket(bucketName).build();
            customMinioClient.makeBucket(makeBucketArgs);
            return bucketName;
        } catch (Exception e) {
            log.error("创建桶失败：{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }


    /**
     * 根据文件类型获取minio桶名称
     *
     * @param fileType
     * @return
     */
    public String getBucketName(String fileType) {
        try {
            //String bucketName = getProperty(fileType.toLowerCase());

            if (fileType != null && !fileType.equals("")) {
                //判断桶是否存在(不存在创建)
                String bucketName2 = createBucket(fileType.toLowerCase());
                if (bucketName2 != null && !bucketName2.equals("")) {
                    return bucketName2;
                } else {
                    return fileType;
                }
            }

        } catch (Exception e) {

            log.error("Error reading bucket name ");
        }
        return fileType;
    }

    /**
     * 读取配置文件
     *
     * @param fileType
     * @return
     * @throws IOException
     */
    private String getProperty(String fileType) throws IOException {
        Properties SysLocalPropObject = new Properties();
        //判断桶关系配置文件是否为空
        if (SysLocalPropObject.isEmpty()) {
            InputStream is = getClass().getResourceAsStream("/BucketRelation.properties");
            SysLocalPropObject.load(is);
            is.close();
        }
        return SysLocalPropObject.getProperty("bucket." + fileType);
    }

    /**
     * 文件上传
     *
     * @param file 文件
     * @return Boolean
     */
    public String upload(MultipartFile file, String bucketName) {
        String originalFilename = file.getOriginalFilename();
        if (StringUtils.isBlank(originalFilename)) {
            throw new RuntimeException();
        }
        String objectName = file.getName();
        try {
            PutObjectArgs objectArgs = PutObjectArgs.builder().bucket(bucketName).object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1).contentType(file.getContentType()).build();
            //文件名称相同会覆盖
            customMinioClient.putObject(objectArgs);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        // 查看文件地址
        GetPresignedObjectUrlArgs build = new GetPresignedObjectUrlArgs().builder().bucket(bucketName).object(objectName).method(Method.GET).build();
        String url = null;
        try {
            url = customMinioClient.getPresignedObjectUrl(build);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return url;
    }


//    /**
//     * 写入配置文件
//     */
//    public void setProperty(String bucketName) {
//        String tempPath = Objects.requireNonNull(getClass().getResource("/BucketRelation.properties")).getPath();
//        OutputStream os;
//        try {
//            os = new FileOutputStream(tempPath);
//            SysLocalPropObject.setProperty(bucketName, bucketName);
//            SysLocalPropObject.store(os, "Update " + bucketName + " " + bucketName);
//            os.close();
//        } catch (IOException e) {
//        }
//    }
}


//    @Autowired
//    private MinioProp minioProp;
//
//
//    @Autowired
//    private MinioClient minioClient;
//
//
//
//    /**
//     * 列出所有的桶
//     */
//    public List<String> listBuckets() throws Exception {
//        List<Bucket> list = minioClient.listBuckets();
//        List<String> names = new ArrayList<>();
//        list.forEach(b -> {
//            names.add(b.name());
//        });
//        return names;
//    }
//
//    /**
//     * 列出一个桶中的所有文件和目录
//     */
//    public List<Fileinfo> listFiles(String bucket) throws Exception {
//        Iterable<Result<Item>> results = minioClient.listObjects(
//                ListObjectsArgs.builder().bucket(bucket).recursive(true).build());
//
//        List<Fileinfo> infos = new ArrayList<>();
//        results.forEach(r->{
//            Fileinfo info = new Fileinfo();
//            try {
//                Item item = r.get();
//                info.setFilename(item.objectName());
//                info.setDirectory(item.isDir());
//                infos.add(info);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        });
//        return infos;
//    }
//
//    /**
//     * 下载一个文件
//     */
//    public InputStream download(String bucket, String objectName) throws Exception {
//        InputStream stream = minioClient.getObject(
//                GetObjectArgs.builder().bucket(bucket).object(objectName).build());
//        return stream;
//    }
//
//    /**
//     * 删除一个桶
//     */
//    public void deleteBucket(String bucket) throws Exception {
//        minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucket).build());
//    }
//
//    /**
//     * 删除一个对象
//     */
//    public void deleteObject(String bucket, String objectName) throws Exception {
//        minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectName).build());
//    }
//
//
//    /**
//     * 创建一个桶
//     */
//    public void createBucket(String bucketName) {
//        BucketExistsArgs bucketExistsArgs = BucketExistsArgs.builder().bucket(bucketName).build();
//        MakeBucketArgs makeBucketArgs = MakeBucketArgs.builder().bucket(bucketName).build();
//        try {
//            if (minioClient.bucketExists(bucketExistsArgs))
//                return;
//            minioClient.makeBucket(makeBucketArgs);
//        } catch (Exception e) {
//            log.error("创建桶失败：{}", e.getMessage());
//            throw new RuntimeException(e);
//        }
//    }
//
//    /**
//     * 上传一个文件
//     * @param file       文件
//     * @param bucketName 存储桶
//     * @return
//     */
//    public JSONObject uploadFile(MultipartFile file, String bucketName) throws Exception {
//        JSONObject res = new JSONObject();
//        res.put("code", 0);
//        // 判断上传文件是否为空
//        if (null == file || 0 == file.getSize()) {
//            res.put("msg", "上传文件不能为空");
//            return res;
//        }
//        // 判断存储桶是否存在
//        createBucket(bucketName);
//        // 文件名
//        String originalFilename = file.getOriginalFilename();
//        // 新的文件名 = 存储桶名称_时间戳.后缀名
//        String fileName = bucketName + "_" + System.currentTimeMillis() +                              									originalFilename.substring(originalFilename.lastIndexOf("."));
//        // 开始上传
//        InputStream inputStream = file.getInputStream();
//        PutObjectArgs args = PutObjectArgs.builder().bucket(bucketName).object(fileName)
//                .stream(inputStream,inputStream.available(),-1).build();
//        minioClient.putObject(args);
//        res.put("code", 1);
//        res.put("msg", minioProp.getEndpoint() + "/" + bucketName + "/" + fileName);
//        return res;
//    }