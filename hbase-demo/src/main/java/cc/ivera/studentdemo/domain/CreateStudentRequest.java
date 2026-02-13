package cc.ivera.studentdemo.domain;

import lombok.Data;

import javax.validation.constraints.*;

@Data
public class CreateStudentRequest {
  @NotBlank
  @Size(max = 64)
  private String id;

  @NotBlank
  @Size(max = 128)
  private String name;

  @NotNull
  @Min(1)
  @Max(200)
  private Integer age;

  @NotBlank
  @Size(max = 64)
  private String grade;
}
