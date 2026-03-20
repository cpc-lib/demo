package cc.ivera.model.vo;


import lombok.Data;
import lombok.experimental.Accessors;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Accessors(chain = true)
public class FileUploadInfo {

    @NotBlank(message = "文件名不能为空")
    private String fileName;

    @NotNull(message = "文件大小不能为空")
    private Long fileSize;

    @NotBlank(message = "Content-Type不能为空")
    private String contentType;

    @NotNull(message = "分片数量不能为空")
    private Integer chunkNum;

    @NotBlank(message = "uploadId 不能为空")
    private String uploadId;

    private Long chunkSize;

    // 桶名称
    //private String bucketName;

    //md5
    private String fileMd5;

    //文件类型
    private String fileType;

    //已上传的分片索引+1
    private List<Integer> chunkUploadedList;

}

