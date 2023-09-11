package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.StudentDao;
import com.takima.backskeleton.models.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StudentService {
    private final StudentDao studentDao;

    public List<Student> findAll() {
        Iterable<Student> it = studentDao.findAll();
        List <Student> users = new ArrayList<>();
        it.forEach(users::add);
        return users ;
    }

    public void deleteById(Long id) {
        studentDao.deleteById(id);
    }

    public void addStudent(Student student) {
        studentDao.save(student);

    }

    public List<Student> searchByMajorAndCourse(int majorId, int courseId) {
        return studentDao.findByMajorIdAndCourseId(majorId, courseId);
    }
}
