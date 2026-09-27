package com.charan.idcard.repository;

import com.charan.idcard.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {
    // JpaRepository already provides save(), findById(), findAll(), deleteById()
}
