package com.hdms.repo;

import com.hdms.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatientRepo extends JpaRepository<Patient, Long> {
    List<Patient> findByActiveTrueOrderByMrn();
}
