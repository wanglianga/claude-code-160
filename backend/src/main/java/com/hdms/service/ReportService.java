package com.hdms.service;

import com.hdms.model.*;
import com.hdms.model.enums.*;
import com.hdms.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {
    private final ScheduleRepo scheduleRepo;
    private final MachineRepo machineRepo;
    private final ExceptionRepo exceptionRepo;
    private final ConsumableTxnRepo txnRepo;
    private final DisinfectionRepo disinfectionRepo;

    public ReportService(ScheduleRepo scheduleRepo, MachineRepo machineRepo, ExceptionRepo exceptionRepo,
                         ConsumableTxnRepo txnRepo, DisinfectionRepo disinfectionRepo) {
        this.scheduleRepo = scheduleRepo;
        this.machineRepo = machineRepo;
        this.exceptionRepo = exceptionRepo;
        this.txnRepo = txnRepo;
        this.disinfectionRepo = disinfectionRepo;
    }

    /** 管理端总览：机位利用率、异常率、耗材损耗、消毒合规 */
    @Transactional(readOnly = true)
    public Map<String, Object> overview(LocalDate from, LocalDate to) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("utilization", utilization(from, to));
        result.put("exceptions", exceptions(from, to));
        result.put("consumableLoss", consumableLoss(from, to));
        result.put("disinfectionCompliance", disinfectionCompliance(from, to));
        return result;
    }

    /** 机位利用率：实际使用机时 / 可用机时（机器数 × 班次窗口） */
    private Map<String, Object> utilization(LocalDate from, LocalDate to) {
        List<Schedule> schedules = scheduleRepo.findByScheduleDateBetween(from, to).stream()
                .filter(s -> s.getStatus() != ScheduleStatus.CANCELLED && s.getStatus() != ScheduleStatus.ABSENT)
                .toList();
        long machineCount = machineRepo.findByActiveTrueOrderByCode().size();
        long days = from.datesUntil(to.plusDays(1)).count();
        int shiftWindowMin = Arrays.stream(Shift.values())
                .mapToInt(s -> (int) java.time.Duration.between(s.getStart(), s.getEnd()).toMinutes()).sum();
        long capacityMin = machineCount * days * shiftWindowMin;
        long usedMin = schedules.stream().mapToLong(Schedule::getPlannedDurationMin).sum();

        Map<LocalDate, Long> byDay = schedules.stream().collect(Collectors.groupingBy(
                Schedule::getScheduleDate, TreeMap::new, Collectors.summingLong(Schedule::getPlannedDurationMin)));
        List<Map<String, Object>> daily = new ArrayList<>();
        long dailyCapacity = machineCount * shiftWindowMin;
        byDay.forEach((d, used) -> daily.add(Map.of(
                "date", d.toString(),
                "usedMin", used,
                "rate", dailyCapacity == 0 ? 0 : round(used * 100.0 / dailyCapacity))));

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("usedMin", usedMin);
        m.put("capacityMin", capacityMin);
        m.put("rate", capacityMin == 0 ? 0 : round(usedMin * 100.0 / capacityMin));
        m.put("sessionCount", schedules.size());
        m.put("daily", daily);
        return m;
    }

    /** 异常率：异常数 / 治疗数，按类型分布 */
    private Map<String, Object> exceptions(LocalDate from, LocalDate to) {
        List<ExceptionEvent> events = exceptionRepo.findByCreatedAtBetween(
                LocalDateTime.of(from, LocalTime.MIN), LocalDateTime.of(to, LocalTime.MAX));
        Map<String, Long> byType = events.stream().collect(Collectors.groupingBy(
                e -> e.getType().name(), LinkedHashMap::new, Collectors.counting()));
        Map<String, Long> byStatus = events.stream().collect(Collectors.groupingBy(
                e -> e.getStatus().name(), LinkedHashMap::new, Collectors.counting()));
        long treatments = scheduleRepo.findByScheduleDateBetween(from, to).stream()
                .filter(s -> s.getStatus() == ScheduleStatus.COMPLETED).count();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", events.size());
        m.put("completedTreatments", treatments);
        m.put("rate", treatments == 0 ? 0 : round(events.size() * 100.0 / treatments));
        m.put("byType", byType);
        m.put("byStatus", byStatus);
        return m;
    }

    /** 耗材损耗：LOSS 流水数量与金额 */
    private Map<String, Object> consumableLoss(LocalDate from, LocalDate to) {
        List<ConsumableTxn> losses = txnRepo.findByCreatedAtBetween(
                        LocalDateTime.of(from, LocalTime.MIN), LocalDateTime.of(to, LocalTime.MAX)).stream()
                .filter(t -> t.getType() == TxnType.LOSS).toList();
        BigDecimal amount = BigDecimal.ZERO;
        Map<String, Integer> byItem = new LinkedHashMap<>();
        for (ConsumableTxn t : losses) {
            amount = amount.add(t.getConsumable().getUnitPrice().multiply(BigDecimal.valueOf(t.getQuantity())));
            byItem.merge(t.getConsumable().getName(), t.getQuantity(), Integer::sum);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("count", losses.stream().mapToInt(ConsumableTxn::getQuantity).sum());
        m.put("amount", amount.setScale(2, RoundingMode.HALF_UP));
        m.put("byItem", byItem);
        return m;
    }

    /** 消毒合规：有治疗完成的机器-日期中，当日有合格消毒记录的比例 */
    private Map<String, Object> disinfectionCompliance(LocalDate from, LocalDate to) {
        List<Schedule> completed = scheduleRepo.findByScheduleDateBetween(from, to).stream()
                .filter(s -> s.getStatus() == ScheduleStatus.COMPLETED).toList();
        Set<String> machineDays = completed.stream()
                .map(s -> s.getMachine().getId() + "@" + s.getScheduleDate()).collect(Collectors.toSet());
        List<DisinfectionRecord> records = disinfectionRepo.findByPerformedAtBetween(
                        LocalDateTime.of(from, LocalTime.MIN), LocalDateTime.of(to, LocalTime.MAX)).stream()
                .filter(r -> r.getType() == DisinfectionType.DISINFECTION && "PASS".equals(r.getResult()))
                .toList();
        Set<String> compliant = records.stream()
                .map(r -> r.getMachine().getId() + "@" + r.getPerformedAt().toLocalDate())
                .collect(Collectors.toSet());
        long ok = machineDays.stream().filter(compliant::contains).count();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("machineDays", machineDays.size());
        m.put("compliant", ok);
        m.put("rate", machineDays.isEmpty() ? 100 : round(ok * 100.0 / machineDays.size()));
        return m;
    }

    private double round(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
