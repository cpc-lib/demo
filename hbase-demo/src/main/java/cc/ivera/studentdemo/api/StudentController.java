package cc.ivera.studentdemo.api;

import cc.ivera.studentdemo.domain.CreateStudentRequest;
import cc.ivera.studentdemo.domain.PageResponse;
import cc.ivera.studentdemo.domain.Student;
import cc.ivera.studentdemo.domain.UpdateStudentRequest;
import cc.ivera.studentdemo.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/students")
@Validated
public class StudentController {

  private final StudentService service;

  @PostMapping
  public Student create(@Valid @RequestBody CreateStudentRequest req) throws Exception {
    return service.create(req);
  }

  @GetMapping("/{id}")
  public Student get(@PathVariable String id) throws Exception {
    return service.get(id);
  }

  @PutMapping("/{id}")
  public Student update(@PathVariable String id, @Valid @RequestBody UpdateStudentRequest req) throws Exception {
    return service.update(id, req);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable String id) throws Exception {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }

  /**
   * Cursor pagination:
   * GET /api/students?pageSize=10&cursor=stu%23S001
   */
  @GetMapping
  public PageResponse<Student> list(@RequestParam(defaultValue = "10") int pageSize,
                                    @RequestParam(required = false) String cursor) throws Exception {
    return service.list(pageSize, cursor);
  }
}
