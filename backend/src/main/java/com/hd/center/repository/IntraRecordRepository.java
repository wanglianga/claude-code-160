package com.hd.center.repository;

import com.hd.center.entity.IntraRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IntraRecordRepository extends JpaRepository<IntraRecord, Long> {
    List<IntraRecord> findBySessionIdOrderByRecordTime(Long sessionId);
}
