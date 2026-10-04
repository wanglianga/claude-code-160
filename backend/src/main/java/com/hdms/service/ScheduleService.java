package com.hdms.service;

import com.hdms.config.GlobalExceptionHandler.BizException;
import com.hdms.config.SecurityUtils;
import com.hdms.model.*;
import com.hdms.model.enums.*;
import com.hdms.repo.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class ScheduleService {
    private final ScheduleRepo scheduleRepo;
    private final PatientRepo patientRepo;
    private final MachineRepo machineRepo;
    private final TreatmentRepo treatmentRepo;
    private final ConsumableTxnRepo txnRepo;
    private final ConsumableService consumableService;
    private final ExceptionService exceptionService;
    private final int gapMinutes;

    public ScheduleService(ScheduleRepo scheduleRepo, PatientRepo patientRepo, MachineRepo machineRepo,
                           TreatmentRepo treatmentRepo, ConsumableTxnRepo txnRepo,
                           ConsumableService consumableService, ExceptionService exceptionService,
                           @Value("${app.disinfection-gap-minutes:30}") int gapMinutes) {
        this.scheduleRepo = scheduleRepo;
        this.patientRepo = patientRepo;
        this.machineRepo = machineRepo;
        this.treatmentRepo = treatmentRepo;
        this.txnRepo = txnRepo;
        this.consumableService = consumableService;
        this.exceptionService = exceptionService;
        this.gapMinutes = gapMinutes;
    }

    // ---------------- 看板 ----------------

    @Transactional(readOnly = true)
    public Map<String, Object> board(LocalDate date) {
        List<Machine> machines = machineRepo.findByActiveTrueOrderByCode();
        List<Schedule> daySchedules = scheduleRepo.findByScheduleDateOrderByShiftAscSeqOrderAsc(date);
        Map<Long, TreatmentRecord> treatmentBySchedule = new HashMap<>();
        treatmentRepo.findByScheduleScheduleDate(date)
                .forEach(t -> treatmentBySchedule.put(t.getSchedule().getId(), t));

        List<Map<String, Object>> machineRows = new ArrayList<>();
        for (Machine m : machines) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("machine", m);
            Map<String, List<Map<String, Object>>> cells = new LinkedHashMap<>();
            for (Shift s : Shift.values()) cells.put(s.name(), new ArrayList<>());
            for (Schedule sc : daySchedules) {
                if (sc.getMachine().getId().equals(m.getId())) {
                    cells.get(sc.getShift().name()).add(toDto(sc, treatmentBySchedule.get(sc.getId())));
                }
            }
            row.put("cells", cells);
            machineRows.add(row);
        }

        // 当日规律透析但尚未排班的患者
        String dow = dayCode(date);
        Set<Long> scheduledPatientIds = new HashSet<>();
        daySchedules.forEach(s -> {
            if (s.getStatus() != ScheduleStatus.CANCELLED) scheduledPatientIds.add(s.getPatient().getId());
        });
        List<Patient> unscheduled = patientRepo.findByActiveTrueOrderByMrn().stream()
                .filter(p -> p.getPatternDays() != null && p.getPatternDays().contains(dow))
                .filter(p -> !scheduledPatientIds.contains(p.getId()))
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", date.toString());
        result.put("shifts", Shift.values());
        result.put("machines", machineRows);
        result.put("unscheduled", unscheduled);
        result.put("gapMinutes", gapMinutes);
        return result;
    }

    private Map<String, Object> toDto(Schedule sc, TreatmentRecord t) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", sc.getId());
        dto.put("patientId", sc.getPatient().getId());
        dto.put("patientName", sc.getPatient().getName());
        dto.put("mrn", sc.getPatient().getMrn());
        dto.put("riskLevel", sc.getPatient().getRiskLevel().name());
        dto.put("infectionZone", sc.getPatient().getInfectionZone().name());
        dto.put("transportNeeded", sc.getPatient().isTransportNeeded());
        dto.put("transportSlot", sc.getTransportSlot());
        dto.put("machineId", sc.getMachine().getId());
        dto.put("machineCode", sc.getMachine().getCode());
        dto.put("shift", sc.getShift().name());
        dto.put("seqOrder", sc.getSeqOrder());
        dto.put("status", sc.getStatus().name());
        dto.put("plannedDurationMin", sc.getPlannedDurationMin());
        dto.put("note", sc.getNote());
        if (t != null) {
            dto.put("treatmentId", t.getId());
            dto.put("treatmentStatus", t.getStatus().name());
        }
        return dto;
    }

    // ---------------- 手动排班 ----------------

    @Transactional
    public Schedule create(Long patientId, Long machineId, LocalDate date, Shift shift,
                           Integer durationMin, String note) {
        Patient patient = patientRepo.findById(patientId).orElseThrow(() -> new BizException("患者不存在"));
        Machine machine = machineRepo.findById(machineId).orElseThrow(() -> new BizException("机器不存在"));
        validateAssignable(patient, machine);
        scheduleRepo.findByPatientIdAndScheduleDateAndShift(patientId, date, shift).ifPresent(s -> {
            throw new BizException("该患者当日此班次已有排班");
        });

        Schedule sc = new Schedule();
        sc.setPatient(patient);
        sc.setMachine(machine);
        sc.setScheduleDate(date);
        sc.setShift(shift);
        sc.setPlannedDurationMin(durationMin != null ? durationMin :
                patient.getUsualDurationMin() != null ? patient.getUsualDurationMin() : 240);
        sc.setSeqOrder(nextSeq(machineId, date, shift));
        sc.setTransportSlot(patient.isTransportNeeded() ? patient.getTransportWindow() : null);
        sc.setNote(note);
        sc.setCreatedBy(SecurityUtils.currentUser());
        return scheduleRepo.save(sc);
    }

    private void validateAssignable(Patient patient, Machine machine) {
        if (!machine.isActive()) throw new BizException("机器已停用");
        if (machine.getStatus() == MachineStatus.FAULT) throw new BizException("机器故障中，不可排班");
        if (machine.getStatus() == MachineStatus.MAINTENANCE) throw new BizException("机器维护中，不可排班");
        if (patient.getInfectionZone() != machine.getZone()) {
            throw new BizException("感染分区不匹配：患者为" + patient.getInfectionZone()
                    + "区，机器为" + machine.getZone() + "区");
        }
    }

    private int nextSeq(Long machineId, LocalDate date, Shift shift) {
        return (int) scheduleRepo.findByScheduleDateAndMachineId(date, machineId).stream()
                .filter(s -> s.getShift() == shift && s.getStatus() != ScheduleStatus.CANCELLED)
                .count() + 1;
    }

    // ---------------- 自动排班 ----------------

    @Transactional
    public Map<String, Object> autoArrange(LocalDate date, Shift shift) {
        String dow = dayCode(date);
        List<Schedule> daySchedules = scheduleRepo.findByScheduleDateOrderByShiftAscSeqOrderAsc(date);
        Set<Long> scheduledPatients = new HashSet<>();
        daySchedules.forEach(s -> {
            if (s.getStatus() != ScheduleStatus.CANCELLED) scheduledPatients.add(s.getPatient().getId());
        });

        // 候选患者：当日规律透析、班次匹配、尚未排班；按风险与接送窗口排序
        List<Patient> candidates = patientRepo.findByActiveTrueOrderByMrn().stream()
                .filter(p -> p.getPatternDays() != null && p.getPatternDays().contains(dow))
                .filter(p -> p.getPreferredShift() == null || p.getPreferredShift() == shift)
                .filter(p -> !scheduledPatients.contains(p.getId()))
                .sorted(Comparator
                        .comparingInt((Patient p) -> riskRank(p.getRiskLevel()))
                        .thenComparing(p -> p.isTransportNeeded() ? 0 : 1)
                        .thenComparing(p -> Optional.ofNullable(p.getTransportWindow()).orElse("99:99")))
                .toList();

        List<Machine> machines = machineRepo.findByActiveTrueOrderByCode().stream()
                .filter(m -> m.getStatus() != MachineStatus.FAULT && m.getStatus() != MachineStatus.MAINTENANCE)
                .toList();

        int shiftWindowMin = (int) java.time.Duration.between(shift.getStart(), shift.getEnd()).toMinutes();
        // 每台机器当班次已占用分钟（含消毒间隔）
        Map<Long, Integer> usedMin = new HashMap<>();
        Map<Long, Integer> seq = new HashMap<>();
        for (Schedule s : daySchedules) {
            if (s.getShift() == shift && s.getStatus() != ScheduleStatus.CANCELLED) {
                usedMin.merge(s.getMachine().getId(), s.getPlannedDurationMin() + gapMinutes, Integer::sum);
                seq.merge(s.getMachine().getId(), 1, Integer::sum);
            }
        }

        List<Map<String, Object>> assigned = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        User operator = SecurityUtils.currentUser();
        for (Patient p : candidates) {
            int duration = p.getUsualDurationMin() != null ? p.getUsualDurationMin() : 240;
            Machine best = null;
            int bestUsed = Integer.MAX_VALUE;
            for (Machine m : machines) {
                if (m.getZone() != p.getInfectionZone()) continue;
                int used = usedMin.getOrDefault(m.getId(), 0);
                if (used + duration <= shiftWindowMin && used < bestUsed) {
                    best = m;
                    bestUsed = used;
                }
            }
            if (best == null) {
                skipped.add(p.getName() + "（无匹配" + p.getInfectionZone() + "区空机）");
                continue;
            }
            Schedule sc = new Schedule();
            sc.setPatient(p);
            sc.setMachine(best);
            sc.setScheduleDate(date);
            sc.setShift(shift);
            sc.setPlannedDurationMin(duration);
            sc.setSeqOrder(seq.getOrDefault(best.getId(), 0) + 1);
            sc.setTransportSlot(p.isTransportNeeded() ? p.getTransportWindow() : null);
            sc.setCreatedBy(operator);
            scheduleRepo.save(sc);
            usedMin.merge(best.getId(), duration + gapMinutes, Integer::sum);
            seq.merge(best.getId(), 1, Integer::sum);
            assigned.add(toDto(sc, null));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("assigned", assigned);
        result.put("skipped", skipped);
        return result;
    }

    private int riskRank(RiskLevel r) {
        return switch (r) { case HIGH -> 0; case MEDIUM -> 1; case LOW -> 2; };
    }

    // ---------------- 调班影响预览 / 执行 ----------------

    @Transactional(readOnly = true)
    public Map<String, Object> movePreview(Long scheduleId, Long toMachineId, Shift toShift) {
        Schedule sc = scheduleRepo.findById(scheduleId).orElseThrow(() -> new BizException("排班不存在"));
        Machine target = machineRepo.findById(toMachineId).orElseThrow(() -> new BizException("目标机器不存在"));

        Map<String, Object> result = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        if (sc.getPatient().getInfectionZone() != target.getZone()) {
            errors.add("感染分区不匹配：患者为" + sc.getPatient().getInfectionZone() + "区，目标机器为" + target.getZone() + "区");
        }
        if (target.getStatus() == MachineStatus.FAULT) errors.add("目标机器故障中");
        if (target.getStatus() == MachineStatus.MAINTENANCE) errors.add("目标机器维护中");

        // 目标机位当班次现有患者
        List<Map<String, Object>> targetQueue = scheduleRepo
                .findByScheduleDateAndMachineId(sc.getScheduleDate(), toMachineId).stream()
                .filter(s -> s.getShift() == toShift && s.getStatus() != ScheduleStatus.CANCELLED)
                .sorted(Comparator.comparing(Schedule::getSeqOrder))
                .map(s -> {
                    Map<String, Object> m = new LinkedHashMap<String, Object>();
                    m.put("patientName", s.getPatient().getName());
                    m.put("seqOrder", s.getSeqOrder());
                    m.put("durationMin", s.getPlannedDurationMin());
                    m.put("status", s.getStatus().name());
                    return m;
                }).toList();

        // 模拟目标机器全天时间线（插入该患者后）
        List<Schedule> simulated = new ArrayList<>(
                scheduleRepo.findByScheduleDateAndMachineId(sc.getScheduleDate(), toMachineId).stream()
                        .filter(s -> s.getStatus() != ScheduleStatus.CANCELLED).toList());
        Schedule ghost = new Schedule();
        ghost.setPatient(sc.getPatient());
        ghost.setMachine(target);
        ghost.setScheduleDate(sc.getScheduleDate());
        ghost.setShift(toShift);
        ghost.setPlannedDurationMin(sc.getPlannedDurationMin());
        ghost.setSeqOrder(999);
        simulated.add(ghost);
        List<Map<String, Object>> timelineIssues = simulateTimeline(simulated);

        // 已预领耗材
        List<Map<String, Object>> preDrawn = txnRepo.findByScheduleIdAndType(sc.getId(), TxnType.PRE_DRAW).stream()
                .map(t -> {
                    Map<String, Object> m = new LinkedHashMap<String, Object>();
                    m.put("consumable", t.getConsumable().getName());
                    m.put("quantity", t.getQuantity());
                    m.put("unit", t.getConsumable().getUnit());
                    return m;
                }).toList();

        // 受影响患者（时间线出现问题的患者）
        Set<String> affected = new LinkedHashSet<>();
        timelineIssues.forEach(i -> affected.add((String) i.get("patientName")));

        result.put("errors", errors);
        result.put("targetMachine", target.getCode());
        result.put("targetShift", toShift.name());
        result.put("targetQueue", targetQueue);
        result.put("timelineIssues", timelineIssues);
        result.put("preDrawnConsumables", preDrawn);
        result.put("affectedPatients", affected);
        result.put("movedPatient", sc.getPatient().getName());
        return result;
    }

    /**
     * 模拟一台机器当天的上机时间线：同班次内按 seqOrder 依次上机，
     * 两台次之间需留出消毒间隔；报告开机延误与超出班次窗口。
     */
    private List<Map<String, Object>> simulateTimeline(List<Schedule> sessions) {
        List<Map<String, Object>> issues = new ArrayList<>();
        Map<Shift, List<Schedule>> byShift = new EnumMap<>(Shift.class);
        for (Schedule s : sessions) {
            byShift.computeIfAbsent(s.getShift(), k -> new ArrayList<>()).add(s);
        }
        LocalDateTime prevEnd = null;
        String prevPatient = null;
        for (Shift shift : Shift.values()) {
            List<Schedule> list = byShift.getOrDefault(shift, List.of()).stream()
                    .sorted(Comparator.comparing(Schedule::getSeqOrder)).toList();
            if (list.isEmpty()) continue;
            LocalDate date = sessions.get(0).getScheduleDate();
            LocalDateTime cursor = LocalDateTime.of(date, shift.getStart());
            for (Schedule s : list) {
                LocalDateTime start = cursor;
                if (prevEnd != null) {
                    LocalDateTime earliest = prevEnd.plusMinutes(gapMinutes);
                    if (earliest.isAfter(start)) {
                        start = earliest;
                        issues.add(issue(s.getPatient().getName(), shift,
                                "受上一台次（" + prevPatient + "）消毒间隔影响，开机推迟至 " + start.toLocalTime()));
                    }
                }
                LocalDateTime end = start.plusMinutes(s.getPlannedDurationMin());
                LocalDateTime shiftEnd = LocalDateTime.of(date, shift.getEnd());
                if (end.isAfter(shiftEnd)) {
                    long over = java.time.Duration.between(shiftEnd, end).toMinutes();
                    issues.add(issue(s.getPatient().getName(), shift,
                            "预计下机 " + end.toLocalTime() + "，超出班次窗口 " + over + " 分钟，将压缩后续消毒/接诊时间"));
                }
                prevEnd = end;
                prevPatient = s.getPatient().getName();
                cursor = start.plusMinutes(s.getPlannedDurationMin() + gapMinutes);
            }
        }
        return issues;
    }

    private Map<String, Object> issue(String patientName, Shift shift, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("patientName", patientName);
        m.put("shift", shift.name());
        m.put("message", message);
        return m;
    }

    @Transactional
    public Schedule move(Long scheduleId, Long toMachineId, Shift toShift) {
        Schedule sc = scheduleRepo.findById(scheduleId).orElseThrow(() -> new BizException("排班不存在"));
        if (sc.getStatus() == ScheduleStatus.COMPLETED || sc.getStatus() == ScheduleStatus.ON_DIALYSIS) {
            throw new BizException("治疗已开始或已完成，不可调班");
        }
        Machine target = machineRepo.findById(toMachineId).orElseThrow(() -> new BizException("目标机器不存在"));
        validateAssignable(sc.getPatient(), target);
        sc.setMachine(target);
        sc.setShift(toShift);
        sc.setSeqOrder(nextSeq(toMachineId, sc.getScheduleDate(), toShift));
        sc.setUpdatedAt(LocalDateTime.now());
        return scheduleRepo.save(sc);
    }

    @Transactional
    public Schedule cancel(Long scheduleId, String reason) {
        Schedule sc = scheduleRepo.findById(scheduleId).orElseThrow(() -> new BizException("排班不存在"));
        if (sc.getStatus() == ScheduleStatus.ON_DIALYSIS) throw new BizException("透析中不可取消，请先完成下机记录");
        sc.setStatus(ScheduleStatus.CANCELLED);
        sc.setCancelReason(reason);
        sc.setUpdatedAt(LocalDateTime.now());
        // 已预领耗材自动退回库存
        consumableService.returnPreDrawn(sc, "排班取消自动退回");
        return scheduleRepo.save(sc);
    }

    @Transactional
    public Schedule markAbsent(Long scheduleId, String note) {
        Schedule sc = scheduleRepo.findById(scheduleId).orElseThrow(() -> new BizException("排班不存在"));
        sc.setStatus(ScheduleStatus.ABSENT);
        sc.setCancelReason(note);
        sc.setUpdatedAt(LocalDateTime.now());
        consumableService.returnPreDrawn(sc, "患者未到自动退回");
        exceptionService.create(ExceptionType.LEAVE, sc, sc.getPatient(), sc.getMachine(), null,
                "患者临时请假/未到：" + (note == null ? "" : note));
        return scheduleRepo.save(sc);
    }

    @Transactional
    public void markLate(Long scheduleId, String note) {
        Schedule sc = scheduleRepo.findById(scheduleId).orElseThrow(() -> new BizException("排班不存在"));
        exceptionService.create(ExceptionType.LATE, sc, sc.getPatient(), sc.getMachine(), null,
                "患者迟到：" + (note == null ? "" : note));
    }

    public static String dayCode(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "MON"; case TUESDAY -> "TUE"; case WEDNESDAY -> "WED";
            case THURSDAY -> "THU"; case FRIDAY -> "FRI"; case SATURDAY -> "SAT";
            case SUNDAY -> "SUN";
        };
    }
}
