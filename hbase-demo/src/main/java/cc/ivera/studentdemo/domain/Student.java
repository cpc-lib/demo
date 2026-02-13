package cc.ivera.studentdemo.domain;

import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {
  private String id;
  private String name;
  private Integer age;
  private String grade;
  private Instant createdAt;
  private Instant updatedAt;
}
