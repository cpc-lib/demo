package cc.ivera.studentdemo.service;

import cc.ivera.studentdemo.domain.CreateStudentRequest;
import cc.ivera.studentdemo.domain.PageResponse;
import cc.ivera.studentdemo.domain.Student;
import cc.ivera.studentdemo.domain.UpdateStudentRequest;
import cc.ivera.studentdemo.hbase.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentService {

  private final StudentRepository repo;

  public Student create(CreateStudentRequest req) throws Exception {
    if (repo.exists(req.getId())) {
      throw new IllegalStateException("student_exists");
    }
    Student s = Student.builder()
        .id(req.getId())
        .name(req.getName())
        .age(req.getAge())
        .grade(req.getGrade())
        .build();
    repo.create(s);
    return repo.get(req.getId());
  }

  public Student get(String id) throws Exception {
    Student s = repo.get(id);
    if (s == null) throw new IllegalStateException("not_found");
    return s;
  }

  public Student update(String id, UpdateStudentRequest req) throws Exception {
    Student cur = repo.get(id);
    if (cur == null) throw new IllegalStateException("not_found");
    repo.update(id, Student.builder().name(req.getName()).age(req.getAge()).grade(req.getGrade()).build());
    return repo.get(id);
  }

  public void delete(String id) throws Exception {
    Student cur = repo.get(id);
    if (cur == null) throw new IllegalStateException("not_found");
    repo.delete(id);
  }

  public PageResponse<Student> list(int pageSize, String cursor) throws Exception {
    return repo.list(pageSize, cursor);
  }
}
