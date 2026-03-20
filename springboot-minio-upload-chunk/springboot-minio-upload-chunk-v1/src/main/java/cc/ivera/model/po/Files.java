package cc.ivera.model.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 
 * </p>
 *
 * @author LGY
 */
@Data
@TableName("files")
public class Files implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("upload_id")
    private String uploadId;

    @TableField("file_md5")
    private String fileMd5;

    @TableField("url")
    private String url;

    @TableField("file_name")
    private String fileName;

    @TableField("bucket_name")
    private String bucketName;

    @TableField("file_type")
    private String fileType;

    @TableField("file_size")
    private Long fileSize;

    @TableField("chunk_size")
    private Long chunkSize;

    @TableField("chunk_num")
    private Integer chunkNum;

    @TableField("is_delete")
    @TableLogic(value = "0",delval = "1")
    private Boolean isDelete;

    @TableField("enable")
    private Boolean enable;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;


}
