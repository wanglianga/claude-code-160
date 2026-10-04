package com.hd.center.repository;

import com.hd.center.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    List<Patient> findByActiveTrueOrderByMrn();
    List<Patient> findByNameContainingOrMrnContainingOrderByMrn(String name, String mrn);
}
