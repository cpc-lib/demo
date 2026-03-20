package cc.ivera.entity;


import lombok.Data;

import java.io.Serializable;

@Data
public class FileRecord implements Serializable {
    private Long id;
    private String fileName;
    private String fileId;

}
