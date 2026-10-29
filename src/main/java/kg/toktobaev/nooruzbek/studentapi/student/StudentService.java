package kg.toktobaev.nooruzbek.studentapi.student;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository repository;

    public StudentService(StudentRepository repository) {
        this.repository = repository;
    }

    public List<Student> findAll() {
        return repository.findAll();
    }

    public Student findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new StudentNotFoundException(id));
    }

    @Transactional
    public Student create(StudentRequest request) {
        Student student = new Student(request.firstName(), request.lastName(), request.email(),
                request.groupName(), request.age());
        return repository.save(student);
    }

    @Transactional
    public Student update(Long id, StudentRequest request) {
        Student student = findById(id);
        student.setFirstName(request.firstName());
        student.setLastName(request.lastName());
        student.setEmail(request.email());
        student.setGroupName(request.groupName());
        student.setAge(request.age());
        return repository.save(student);
    }

    @Transactional
    public void delete(Long id) {
        Student student = findById(id);
        repository.delete(student);
    }
}
