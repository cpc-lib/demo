package cc.ivera.service.impl;

import cc.ivera.dto.FileChunkDTO;
import cc.ivera.dto.FileChunkResultDTO;
import cc.ivera.response.error.BusinessErrorCode;
import cc.ivera.response.error.BusinessException;
import cc.ivera.service.IUploadService;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.*;


/**
 * @Author zhangyukang
 * @Date 2021/1/17 10:24
 * @Version 1.0
 **/
@Service
@SuppressWarnings("all")
public class UploadServiceImpl implements IUploadService {

    private Logger logger = LoggerFactory.getLogger(UploadServiceImpl.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 默认文件存放位置 这个文件目录会自动进行创建
     */
    @Value("${spring.media.path}")
    private String uploadFolder = "mediaCenter";

    /**
     * 检查文件是否存在，如果存在则跳过该文件的上传，如果不存在，返回需要上传的分片集合
     *
     * @param chunkDTO
     * @return
     */
    @Override
    @SuppressWarnings("all")
    public FileChunkResultDTO checkChunkExist(FileChunkDTO chunkDTO) throws BusinessException {
        //1.检查文件是否已上传过
        //1.1)检查在磁盘中是否存在
        String fileFolderPath = getFileFolderPath(chunkDTO.getIdentifier());
        String filePath = getFilePath(chunkDTO.getIdentifier(), chunkDTO.getFilename());
        File file = new File(filePath);
        boolean exists = file.exists();
        //1.2)检查Redis中是否存在,并且所有分片已经上传完成。
        Set<Integer> uploaded = (Set<Integer>) redisTemplate.opsForHash().get(chunkDTO.getIdentifier(), "uploaded");
        if (uploaded != null && uploaded.size() == chunkDTO.getTotalChunks() && exists) {
            return new FileChunkResultDTO(true);
        }
        File fileFolder = new File(fileFolderPath);
        if (!fileFolder.exists()) {
            boolean mkdirs = fileFolder.mkdirs();
            logger.info("准备工作,创建文件夹,fileFolderPath:{},mkdirs:{}", fileFolderPath, mkdirs);
        }
        return new FileChunkResultDTO(false, uploaded);
    }


    /**
     * 上传分片
     *
     * @param chunkDTO
     */
    @Override
    public void uploadChunk(FileChunkDTO chunkDTO) throws BusinessException {
        //分块的目录
        String chunkFileFolderPath = getChunkFileFolderPath(chunkDTO.getIdentifier());
        File chunkFileFolder = new File(chunkFileFolderPath);
        if (!chunkFileFolder.exists()) {
            boolean mkdirs = chunkFileFolder.mkdirs();
            logger.info("创建分片文件夹:{}", mkdirs);
        }
        //写入分片
        try (
                InputStream inputStream = chunkDTO.getFile().getInputStream();
                FileOutputStream outputStream = new FileOutputStream(new File(chunkFileFolderPath + chunkDTO.getChunkNumber()))
        ) {
            IOUtils.copy(inputStream, outputStream);
            logger.info("文件标识:{},chunkNumber:{}", chunkDTO.getIdentifier(), chunkDTO.getChunkNumber());
            //将该分片写入redis
            long size = saveToRedis(chunkDTO);// redis hash结构的使用
            //TODO 合并分片 最后一个分片执行 后台进行合并该md5文件的全部分片信息
            if (size == chunkDTO.getTotalChunks()) {
                // 合并
                File mergeFile = mergeChunks(chunkDTO.getIdentifier(), chunkDTO.getFilename());
                if (mergeFile == null) {
                    throw new BusinessException(BusinessErrorCode.INVALID_PARAMETER, "合并文件失败");
                }
            }
        } catch (Exception e) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAMETER, e.getMessage());
        }
    }

    /**
     * 合并分片
     *
     * @param identifier
     * @param filename
     */
    private File mergeChunks(String identifier, String filename) throws BusinessException {
        String chunkFileFolderPath = getChunkFileFolderPath(identifier);
        String filePath = getFilePath(identifier, filename);
        File chunkFileFolder = new File(chunkFileFolderPath);
        File mergeFile = new File(filePath);
        File[] chunks = chunkFileFolder.listFiles();
        //排序
        Arrays.stream(chunks).sorted(Comparator.comparing(o -> Integer.valueOf(o.getName())));
        try {
            RandomAccessFile randomAccessFileWriter = new RandomAccessFile(mergeFile, "rw");
            byte[] bytes = new byte[1024];
            for (File chunk : chunks) {
                RandomAccessFile randomAccessFileReader = new RandomAccessFile(chunk, "r");
                int len;
                while ((len = randomAccessFileReader.read(bytes)) != -1) {
                    randomAccessFileWriter.write(bytes, 0, len);
                }
                randomAccessFileReader.close();
            }
            randomAccessFileWriter.close();
        } catch (Exception e) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAMETER);
        }
        return mergeFile;
    }

    /**
     * 分片写入Redis
     *
     * @param chunkDTO
     */
    @SuppressWarnings("all")
    private synchronized long saveToRedis(FileChunkDTO chunkDTO) {
        Set<Integer> uploaded = (Set<Integer>) redisTemplate.opsForHash().get(chunkDTO.getIdentifier(), "uploaded");
        if (uploaded == null) {
            uploaded = new HashSet<>(Arrays.asList(chunkDTO.getChunkNumber()));
            HashMap<String, Object> objectObjectHashMap = new HashMap<>();
            objectObjectHashMap.put("uploaded", uploaded);
            objectObjectHashMap.put("totalChunks", chunkDTO.getTotalChunks());
            objectObjectHashMap.put("totalSize", chunkDTO.getTotalSize());
            objectObjectHashMap.put("path", getFileRelativelyPath(chunkDTO.getIdentifier(), chunkDTO.getFilename()));
            redisTemplate.opsForHash().putAll(chunkDTO.getIdentifier(), objectObjectHashMap);
        } else {
            uploaded.add(chunkDTO.getChunkNumber());
            redisTemplate.opsForHash().put(chunkDTO.getIdentifier(), "uploaded", uploaded);
        }
        return uploaded.size();
    }

    /**
     * 得到文件的绝对路径
     *
     * @param identifier
     * @param filename
     * @return
     */
    private String getFilePath(String identifier, String filename) {
        String ext = filename.substring(filename.lastIndexOf("."));
        return getFileFolderPath(identifier) + identifier + ext;
    }

    /**
     * 得到文件的相对路径
     *
     * @param identifier
     * @param filename
     * @return
     */
    private String getFileRelativelyPath(String identifier, String filename) {
        String ext = filename.substring(filename.lastIndexOf("."));
        return "/" + identifier.substring(0, 1) + "/" +
                identifier.substring(1, 2) + "/" +
                identifier + "/" + identifier
                + ext;
    }


    /**
     * 得到分块文件所属的目录
     *
     * @param identifier
     * @return
     */
    private String getChunkFileFolderPath(String identifier) {
        return getFileFolderPath(identifier) + "chunks" + File.separator;
    }

    /**
     * 得到文件所属的目录
     *
     * @param identifier
     * @return
     */
    private String getFileFolderPath(String identifier) {
        return uploadFolder + File.separator + identifier + File.separator;
    }
}
