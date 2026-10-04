package com.hdms.repo;

import com.hdms.model.ConsumableTxn;
import com.hdms.model.enums.TxnType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ConsumableTxnRepo extends JpaRepository<ConsumableTxn, Long> {
    List<ConsumableTxn> findByScheduleId(Long scheduleId);
    List<ConsumableTxn> findByScheduleIdAndType(Long scheduleId, TxnType type);
    List<ConsumableTxn> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
    List<ConsumableTxn> findTop100ByOrderByCreatedAtDesc();
}
