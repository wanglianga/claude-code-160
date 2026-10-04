package com.hdms.repo;

import com.hdms.model.Schedule;
import com.hdms.model.enums.ScheduleStatus;
import com.hdms.model.enums.Shift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepo extends JpaRepository<Schedule, Long> {
    List<Schedule> findByScheduleDateOrderByShiftAscSeqOrderAsc(LocalDate date);
    List<Schedule> findByScheduleDateAndShift(LocalDate date, Shift shift);
    List<Schedule> findByScheduleDateAndMachineId(LocalDate date, Long machineId);
    Optional<Schedule> findByPatientIdAndScheduleDateAndShift(Long patientId, LocalDate date, Shift shift);
    List<Schedule> findByScheduleDateBetween(LocalDate from, LocalDate to);
    long countByScheduleDateAndStatus(LocalDate date, ScheduleStatus status);
    long countByScheduleDate(LocalDate date);
}
