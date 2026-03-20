
package cc.ivera.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("book")
public class Book implements Serializable {

    @TableId("book_id")
    private Long bookId;

    private String bookName;

    private String bookAuthorName;


    /**
     * 标记此字段为逻辑删除字段
     */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private String enabled;


}
