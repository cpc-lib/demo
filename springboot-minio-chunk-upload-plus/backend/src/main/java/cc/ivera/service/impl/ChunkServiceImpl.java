package cc.ivera.service.impl;

import cc.ivera.config.MinioProperties;
import cc.ivera.dto.InitUploadRequest;
import cc.ivera.dto.InitUploadResponse;
import cc.ivera.entity.FileResource;
import cc.ivera.service.ChunkService;
import cc.ivera.service.FileResourceService;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ChunkServiceImpl implements ChunkService {

    private final MinioClient minioClient;
    private final MinioProperties properties;
    private final FileResourceService fileResourceService;


    @Override
    public InitUploadResponse initUpload(InitUploadRequest request) throws Exception {

        // ★★ 先根据 MD5 判断文件是否已经上传过 — 秒传逻辑
        if (request.getFileMd5() != null && !request.getFileMd5().isEmpty()) {
            FileResource exist = fileResourceService.findByMd5(request.getFileMd5());
            if (exist != null) {

                // 生成 5 分钟有效期的预签名 URL
                String url = createPresignedUrl(exist.getObjectName());

                InitUploadResponse resp = new InitUploadResponse();
                resp.setExist(true);
                resp.setFileUrl(url);
                return resp;
            }
        }

        // ★ 原有逻辑：计算 totalChunks + 生成 uploadId
        int totalChunks = (int) Math.ceil((double) request.getFileSize() / request.getChunkSize());
        String uploadId = UUID.randomUUID().toString();

        InitUploadResponse resp = new InitUploadResponse();
        resp.setExist(false);
        resp.setUploadId(uploadId);
        resp.setTotalChunks(totalChunks);
        resp.setBucket(properties.getBucket());
        return resp;
    }

    @Override
    public void uploadChunk(String uploadId, int chunkIndex, MultipartFile chunk) throws Exception {
        String objectName = buildChunkObjectName(uploadId, chunkIndex);
        try (InputStream in = chunk.getInputStream()) {
            minioClient.putObject(
                    io.minio.PutObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectName)
                            .stream(in, chunk.getSize(), -1)
                            .contentType(chunk.getContentType())
                            .build()
            );
        }
    }

    @Override
    public String mergeChunks(String uploadId, int totalChunks, String originalFileName) throws Exception {
        if (totalChunks <= 0) {
            throw new IllegalArgumentException("totalChunks must be > 0");
        }

        // ① 组装分片源
        List<ComposeSource> sources = new ArrayList<>();
        for (int i = 0; i < totalChunks; i++) {
            String chunkObject = buildChunkObjectName(uploadId, i);
            sources.add(ComposeSource.builder()
                    .bucket(properties.getBucket())
                    .object(chunkObject)
                    .build());
        }

        // ② 合并文件
        String finalObjectName = buildFinalObjectName(originalFileName);
        minioClient.composeObject(
                ComposeObjectArgs.builder()
                        .bucket(properties.getBucket())
                        .object(finalObjectName)
                        .sources(sources)
                        .build()
        );

        // ③ 计算文件 MD5
        String md5;
        try (InputStream in = minioClient.getObject(
                GetObjectArgs.builder().bucket(properties.getBucket()).object(finalObjectName).build())) {
            md5 = DigestUtils.md5Hex(in);
        }

        // ④ 判断重复
        FileResource existed = fileResourceService.findByMd5(md5);

        String objectNameToUse;

        if (existed != null) {
            objectNameToUse = existed.getObjectName();

            // 删除刚合并出来的重复文件
            try {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(properties.getBucket())
                                .object(finalObjectName)
                                .build()
                );
            } catch (Exception e) {
                System.err.println("Warning: 删除重复文件失败: " + finalObjectName + " error=" + e.getMessage());
            }
        } else {
            // ⑤ 获取文件大小
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(finalObjectName)
                            .build()
            );
            long fileSize = stat.size();

            // ⑥ 写入数据库
            FileResource fr = new FileResource();
            fr.setFileMd5(md5);
            fr.setFileName(originalFileName);
            fr.setBucket(properties.getBucket());
            fr.setObjectName(finalObjectName);
            fr.setFileUrl(buildFixedUrl(finalObjectName));
            fr.setContentType(Files.probeContentType(Paths.get(originalFileName)));
            fr.setFileSize(fileSize);
            fileResourceService.save(fr);

            objectNameToUse = finalObjectName;
        }

        // ⑦ 删除分片文件（无论秒传还是正常）
        try {
            deleteChunkFolder(uploadId);
        } catch (Exception e) {
            System.err.println("Warning: 分片清理失败 uploadId=" + uploadId + " error=" + e.getMessage());
        }

        // ⑧ 返回访问 URL
        return createPresignedUrl(objectNameToUse);
    }

    /**
     * 删除 MinIO 中 chunk-temp/{uploadId}/ 文件夹下所有分片
     */
    /**
     * 删除 MinIO 中 chunk-temp/{uploadId}/ 下的所有对象（保留 chunk-temp 根目录）
     */
    /**
     * 只删除 chunk-temp/{uploadId} 下所有文件
     * chunk-temp 根目录永久保留
     */
    private void deleteChunkFolder(String uploadId) {
        String prefix = "chunk-temp/" + uploadId + "/";

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(properties.getBucket())
                        .prefix(prefix)
                        .recursive(true)   // 递归删除目录下所有对象
                        .build()
        );

        for (Result<Item> item : results) {
            try {
                String objectName = item.get().objectName();

                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(properties.getBucket())
                                .object(objectName)
                                .build()
                );

                System.out.println("删除分片对象：" + objectName);

            } catch (Exception e) {
                System.err.println("Warning: 删除失败: " + e.getMessage());
            }
        }
    }


    @Override
    public String createPresignedUrl(String objectName) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .bucket(properties.getBucket())
                        .object(objectName)
                        .method(Method.GET)
                        .expiry(1, TimeUnit.MINUTES)
                        .build()
        );
    }

    private String buildChunkObjectName(String uploadId, int chunkIndex) {
        return properties.getChunkPrefix() + uploadId + "/" + chunkIndex;
    }

    private String buildFinalObjectName(String originalFileName) {
        String datePath = LocalDate.now().toString();
        //String safeName = URLEncoder.encode(originalFileName, StandardCharsets.UTF_8);
        //return properties.getFilePrefix() + datePath + "/" + UUID.randomUUID() + "-" + safeName;
        String safeName = originalFileName;
        return properties.getFilePrefix() + datePath + "/" + safeName;
    }

    private String buildFixedUrl(String objectName) {
        return properties.getEndpoint() + "/" + properties.getBucket() + "/" + objectName;
    }
}
