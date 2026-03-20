package cc.ivera.service;

import cc.ivera.dto.FileChunkDTO;
import cc.ivera.dto.FileChunkResultDTO;
import cc.ivera.response.error.BusinessException;

import java.io.IOException;

public interface IUploadService {

    /**
     * 检查文件是否存在，如果存在则跳过该文件的上传，如果不存在，返回需要上传的分片集合
     *
     * @param chunkDTO
     * @return
     */
    FileChunkResultDTO checkChunkExist(FileChunkDTO chunkDTO) throws BusinessException;


    /**
     * 上传文件分片
     *
     * @param chunkDTO
     */
    void uploadChunk(FileChunkDTO chunkDTO) throws BusinessException, IOException;
}
