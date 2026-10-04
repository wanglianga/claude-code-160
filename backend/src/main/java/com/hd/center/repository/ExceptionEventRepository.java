package com.hd.center.repository;

import com.hd.center.entity.ExceptionEvent;
import com.hd.center.entity.Enums.ExceptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ExceptionEventRepository extends JpaRepository<ExceptionEvent, Long> {
    List<ExceptionEvent> findByStatusOrderByCreatedAtDesc(ExceptionStatus status);
    List<ExceptionEvent> findBySessionIdOrderByCreatedAt(Long sessionId);
    List<ExceptionEvent> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
    List<ExceptionEvent> findTop100ByOrderByCreatedAtDesc();
}
