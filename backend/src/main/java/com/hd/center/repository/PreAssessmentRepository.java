package com.hd.center.repository;

import com.hd.center.entity.PreAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PreAssessmentRepository extends JpaRepository<PreAssessment, Long> {
    Optional<PreAssessment> findBySessionId(Long sessionId);
}
