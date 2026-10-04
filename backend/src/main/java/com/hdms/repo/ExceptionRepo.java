package com.hdms.repo;

import com.hdms.model.ExceptionEvent;
import com.hdms.model.enums.ExceptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ExceptionRepo extends JpaRepository<ExceptionEvent, Long> {
    List<ExceptionEvent> findByStatusOrderByCreatedAtDesc(ExceptionStatus status);
    List<ExceptionEvent> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
    List<ExceptionEvent> findTop200ByOrderByCreatedAtDesc();
    long countByStatus(ExceptionStatus status);
}
