package com.hd.center.repository;

import com.hd.center.entity.PostRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostRecordRepository extends JpaRepository<PostRecord, Long> {
    Optional<PostRecord> findBySessionId(Long sessionId);
}
