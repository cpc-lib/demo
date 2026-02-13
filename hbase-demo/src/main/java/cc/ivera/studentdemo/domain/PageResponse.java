package cc.ivera.studentdemo.domain;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {
  private List<T> items;
  /**
   * Opaque cursor for next page. Pass it back as ?cursor=...
   * Null means no more.
   */
  private String nextCursor;
}
