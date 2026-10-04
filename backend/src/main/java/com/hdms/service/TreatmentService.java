package com.hdms.service;

import com.hdms.config.GlobalExceptionHandler.BizException;
import com.hdms.config.SecurityUtils;
import com.hdms.model.*;
import com.hdms.model.enums.*;
import com.hdms.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class TreatmentService {
    private final TreatmentRepo treatmentRepo;
    private final ScheduleRepo scheduleRepo;
    private final MonitoringRepo monitoringRepo;
    private final MachineRepo machineRepo;
    private final ConsumableService consumableService;
    private final ExceptionService exceptionService;
    private final BillingService billingService;

    public TreatmentService(TreatmentRepo treatmentRepo, ScheduleRepo scheduleRepo,
                            MonitoringRepo monitoringRepo, MachineRepo machineRepo,
                            ConsumableService consumableService, ExceptionService exceptionService,
                            BillingService billingService) {
        this.treatmentRepo = treatmentRepo;
        this.scheduleRepo = scheduleRepo;
        this.monitoringRepo = monitoringRepo;
        this.machineRepo = machineRepo;
        this.consumableService = consumableService;
        this.exceptionService = exceptionService;
        this.billingService = billingService;
    }

    @Transactional(readOnly = true)
    public List<TreatmentRecord> ofDate(LocalDate date) {
        return treatmentRepo.findByScheduleScheduleDate(date);
    }

    @Transactional(readOnly = true)
    public TreatmentRecord detail(Long id) {
        return treatmentRepo.findById(id).orElseThrow(() -> new BizException("治疗记录不存在"));
    }

    @Transactional(readOnly = true)
    public List<MonitoringEntry> entries(Long treatmentId) {
        return monitoringRepo.findByTreatmentIdOrderByRecordedAtAsc(treatmentId);
    }

    /** 透析前评估：体重/血压/通路/上次不适/抗凝/是否调整脱水 */
    @Transactional
    public TreatmentRecord preAssess(Long scheduleId, BigDecimal preWeightKg, Integer preBpSys, Integer preBpDia,
                                     String accessCondition, String lastDiscomfort, String anticoagPlan,
                                     Boolean adjustDehydration, Integer plannedUfMl, String preNotes) {
        Schedule sc = scheduleRepo.findById(scheduleId).orElseThrow(() -> new BizException("排班不存在"));
        if (sc.getStatus() != ScheduleStatus.PLANNED) throw new BizException("当前状态不可登记透析前评估");
        treatmentRepo.findByScheduleId(scheduleId).ifPresent(t -> {
            throw new BizException("该排班已存在治疗记录");
        });
        TreatmentRecord t = new TreatmentRecord();
        t.setSchedule(sc);
        t.setNurse(SecurityUtils.currentUser());
        t.setPreWeightKg(preWeightKg);
        t.setPreBpSys(preBpSys);
        t.setPreBpDia(preBpDia);
        t.setAccessCondition(accessCondition);
        t.setLastDiscomfort(lastDiscomfort);
        t.setAnticoagPlan(anticoagPlan);
        t.setAdjustDehydration(Boolean.TRUE.equals(adjustDehydration));
        t.setPlannedUfMl(plannedUfMl);
        t.setPreNotes(preNotes);
        t.setPreRecordedAt(LocalDateTime.now());
        t.setStatus(TreatmentStatus.PRE_RECORDED);
        return treatmentRepo.save(t);
    }

    /** 上机 */
    @Transactional
    public TreatmentRecord start(Long treatmentId) {
        TreatmentRecord t = detail(treatmentId);
        if (t.getStatus() != TreatmentStatus.PRE_RECORDED) throw new BizException("当前状态不可上机");
        t.setStatus(TreatmentStatus.ON_DIALYSIS);
        t.setStartTime(LocalDateTime.now());
        Schedule sc = t.getSchedule();
        sc.setStatus(ScheduleStatus.ON_DIALYSIS);
        sc.setUpdatedAt(LocalDateTime.now());
        scheduleRepo.save(sc);
        Machine m = sc.getMachine();
        m.setStatus(MachineStatus.IN_USE);
        machineRepo.save(m);
        return treatmentRepo.save(t);
    }

    /** 透析中记录：血压/心率/报警/补液/抽血/不适/医嘱变更；低血压自动上报异常 */
    @Transactional
    public MonitoringEntry addEntry(Long treatmentId, EntryType type, Integer bpSys, Integer bpDia,
                                    Integer heartRate, String note) {
        TreatmentRecord t = detail(treatmentId);
        if (t.getStatus() != TreatmentStatus.ON_DIALYSIS) throw new BizException("仅透析中可录入监测记录");
        MonitoringEntry e = new MonitoringEntry();
        e.setTreatment(t);
        e.setType(type);
        e.setBpSys(bpSys);
        e.setBpDia(bpDia);
        e.setHeartRate(heartRate);
        e.setNote(note);
        e.setRecordedBy(SecurityUtils.currentUser());
        monitoringRepo.save(e);

        // 透析中低血压：收缩压 < 90 自动上报
        if (bpSys != null && bpSys < 90) {
            exceptionService.create(ExceptionType.HYPOTENSION, t.getSchedule(), t.getSchedule().getPatient(),
                    t.getSchedule().getMachine(), t,
                    "透析中低血压：收缩压 " + bpSys + " mmHg");
        }
        // 医嘱变更同步上报，通知医生与护士长
        if (type == EntryType.ORDER_CHANGE) {
            exceptionService.create(ExceptionType.ORDER_CHANGE, t.getSchedule(), t.getSchedule().getPatient(),
                    t.getSchedule().getMachine(), t, "医嘱变更：" + (note == null ? "" : note));
        }
        return e;
    }

    /** 下机：记录后体重/止血/离开方式/下次排班；联动机器消毒、耗材转使用、生成账单 */
    @Transactional
    public TreatmentRecord complete(Long treatmentId, BigDecimal postWeightKg, Boolean hemostasisOk,
                                    DepartureMethod departureMethod, String nextScheduleNote, String postNotes) {
        TreatmentRecord t = detail(treatmentId);
        if (t.getStatus() != TreatmentStatus.ON_DIALYSIS) throw new BizException("仅透析中可下机");
        t.setStatus(TreatmentStatus.COMPLETED);
        t.setEndTime(LocalDateTime.now());
        t.setPostWeightKg(postWeightKg);
        t.setHemostasisOk(Boolean.TRUE.equals(hemostasisOk));
        t.setDepartureMethod(departureMethod);
        t.setNextScheduleNote(nextScheduleNote);
        t.setPostNotes(postNotes);

        Schedule sc = t.getSchedule();
        sc.setStatus(ScheduleStatus.COMPLETED);
        sc.setUpdatedAt(LocalDateTime.now());
        scheduleRepo.save(sc);

        // 机器进入待消毒状态
        Machine m = sc.getMachine();
        m.setStatus(MachineStatus.DISINFECTING);
        machineRepo.save(m);

        // 预领耗材转使用
        consumableService.consumePreDrawn(sc);

        // 生成待核对账单
        billingService.createForTreatment(t);
        return treatmentRepo.save(t);
    }

    /** 治疗记录时间线（含监测记录与关联异常），供各角色查看同一条治疗记录 */
    @Transactional(readOnly = true)
    public Map<String, Object> timeline(Long treatmentId) {
        TreatmentRecord t = detail(treatmentId);
        return Map.of(
                "treatment", t,
                "entries", monitoringRepo.findByTreatmentIdOrderByRecordedAtAsc(treatmentId),
                "consumables", consumableService.txnsOfSchedule(t.getSchedule().getId()));
    }
}
