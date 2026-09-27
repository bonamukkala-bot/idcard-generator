package com.charan.idcard.service;

import com.charan.idcard.model.Student;
import com.charan.idcard.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class StudentService {

    @Autowired
    private StudentRepository studentRepository;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public Student saveStudent(Student student, MultipartFile photo) throws IOException {
        if (photo != null && !photo.isEmpty()) {
            Files.createDirectories(Paths.get(uploadDir));

            String originalName = photo.getOriginalFilename();
            String safeName = (originalName == null) ? "photo.jpg" : originalName;
            String filename = UUID.randomUUID() + "_" + safeName;

            Path target = Paths.get(uploadDir, filename);
            Files.copy(photo.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            student.setPhotoPath(target.toString());
        }
        return studentRepository.save(student);
    }

    public Student getById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + id));
    }

    public Iterable<Student> getAll() {
        return studentRepository.findAll();
    }
}