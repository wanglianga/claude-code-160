package com.hd.center.repository;

import com.hd.center.entity.DisinfectionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface DisinfectionRecordRepository extends JpaRepository<DisinfectionRecord, Long> {
    List<DisinfectionRecord> findByMachineIdOrderByDoneAtDesc(Long machineId);
    List<DisinfectionRecord> findByDoneAtBetween(LocalDateTime from, LocalDateTime to);
    List<DisinfectionRecord> findTop50ByOrderByDoneAtDesc();
}
