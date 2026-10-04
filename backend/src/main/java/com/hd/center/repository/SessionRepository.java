package com.hd.center.repository;

import com.hd.center.entity.DialysisSession;
import com.hd.center.entity.Enums.SessionStatus;
import com.hd.center.entity.Enums.Shift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface SessionRepository extends JpaRepository<DialysisSession, Long> {

    List<DialysisSession> findBySessionDateAndShiftOrderBySeqNo(LocalDate date, Shift shift);

    List<DialysisSession> findBySessionDateOrderByShiftAscSeqNoAsc(LocalDate date);

    List<DialysisSession> findBySessionDateAndShiftAndMachineId(LocalDate date, Shift shift, Long machineId);

    List<DialysisSession> findBySessionDateAndMachineIdAndStatusNotIn(LocalDate date, Long machineId, Collection<SessionStatus> status);

    boolean existsByPatientIdAndSessionDateAndShiftAndStatusNotIn(Long patientId, LocalDate date, Shift shift, Collection<SessionStatus> status);

    List<DialysisSession> findBySessionDateBetween(LocalDate from, LocalDate to);

    List<DialysisSession> findByPatientIdOrderBySessionDateDesc(Long patientId);
}
