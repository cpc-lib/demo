package cc.ivera.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private int page;          // zero-based
    private int size;
    private long total;
    private List<T> items;
}
