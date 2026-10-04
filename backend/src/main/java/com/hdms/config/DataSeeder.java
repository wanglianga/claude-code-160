package com.hdms.config;

import com.hdms.model.*;
import com.hdms.model.enums.*;
import com.hdms.repo.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** 演示数据：仅当用户表为空时初始化 */
@Component
public class DataSeeder implements CommandLineRunner {
    private final UserRepo userRepo;
    private final PatientRepo patientRepo;
    private final MachineRepo machineRepo;
    private final ScheduleRepo scheduleRepo;
    private final TreatmentRepo treatmentRepo;
    private final MonitoringRepo monitoringRepo;
    private final ConsumableRepo consumableRepo;
    private final ConsumableTxnRepo txnRepo;
    private final DisinfectionRepo disinfectionRepo;
    private final ExceptionRepo exceptionRepo;
    private final BillingRepo billingRepo;
    private final PasswordEncoder passwordEncoder;
    private final boolean seedEnabled;

    public DataSeeder(UserRepo userRepo, PatientRepo patientRepo, MachineRepo machineRepo,
                      ScheduleRepo scheduleRepo, TreatmentRepo treatmentRepo, MonitoringRepo monitoringRepo,
                      ConsumableRepo consumableRepo, ConsumableTxnRepo txnRepo, DisinfectionRepo disinfectionRepo,
                      ExceptionRepo exceptionRepo, BillingRepo billingRepo, PasswordEncoder passwordEncoder,
                      @Value("${app.seed-demo-data:true}") boolean seedEnabled) {
        this.userRepo = userRepo;
        this.patientRepo = patientRepo;
        this.machineRepo = machineRepo;
        this.scheduleRepo = scheduleRepo;
        this.treatmentRepo = treatmentRepo;
        this.monitoringRepo = monitoringRepo;
        this.consumableRepo = consumableRepo;
        this.txnRepo = txnRepo;
        this.disinfectionRepo = disinfectionRepo;
        this.exceptionRepo = exceptionRepo;
        this.billingRepo = billingRepo;
        this.passwordEncoder = passwordEncoder;
        this.seedEnabled = seedEnabled;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled || userRepo.count() > 0) return;

        // ---- 用户 ----
        User admin = user("admin", "admin123", "系统管理员", Role.ADMIN);
        User headNurse = user("headnurse", "head123", "王护士长", Role.HEAD_NURSE);
        User nurse1 = user("nurse1", "nurse123", "李护士", Role.NURSE);
        User nurse2 = user("nurse2", "nurse123", "赵护士", Role.NURSE);
        User doctor = user("doctor1", "doctor123", "陈医生", Role.DOCTOR);
        User engineer = user("eng1", "eng123", "刘工程师", Role.ENGINEER);
        User finance = user("fin1", "fin123", "周财务", Role.FINANCE);

        // ---- 机器 ----
        Machine m01 = machine("M01", InfectionZone.CLEAN, "费森尤斯 5008S", "A区-01");
        Machine m02 = machine("M02", InfectionZone.CLEAN, "费森尤斯 5008S", "A区-02");
        Machine m03 = machine("M03", InfectionZone.CLEAN, "贝朗 Dialog+", "A区-03");
        Machine m04 = machine("M04", InfectionZone.CLEAN, "贝朗 Dialog+", "A区-04");
        Machine m05 = machine("M05", InfectionZone.CLEAN, "威高 DBB-27C", "A区-05");
        Machine m06 = machine("M06", InfectionZone.CLEAN, "威高 DBB-27C", "A区-06");
        Machine m07 = machine("M07", InfectionZone.HBV, "费森尤斯 4008S", "B区-01");
        Machine m08 = machine("M08", InfectionZone.HBV, "费森尤斯 4008S", "B区-02");
        Machine m09 = machine("M09", InfectionZone.HCV, "贝朗 Dialog+", "C区-01");
        Machine m10 = machine("M10", InfectionZone.HCV, "贝朗 Dialog+", "C区-02");
        m10.setStatus(MachineStatus.FAULT);
        machineRepo.save(m10);

        // ---- 患者（规律透析） ----
        LocalDate today = LocalDate.now();
        String dow = ScheduleServiceDayCode.of(today);
        // 今日规律透析患者
        Patient p1 = patient("P0001", "张国栋", "男", 1958, RiskLevel.HIGH, InfectionZone.CLEAN,
                true, "06:30-07:00", dow, Shift.MORNING, 62.5, 240, "低分子肝素 4000IU", nurse1);
        Patient p2 = patient("P0002", "李秀兰", "女", 1965, RiskLevel.MEDIUM, InfectionZone.CLEAN,
                false, null, dow, Shift.MORNING, 55.0, 240, "普通肝素", nurse1);
        Patient p3 = patient("P0003", "王建军", "男", 1972, RiskLevel.MEDIUM, InfectionZone.CLEAN,
                true, "12:30-13:00", dow, Shift.AFTERNOON, 70.2, 240, "低分子肝素 3000IU", nurse2);
        Patient p4 = patient("P0004", "刘桂芳", "女", 1960, RiskLevel.HIGH, InfectionZone.HBV,
                false, null, dow, Shift.MORNING, 48.6, 210, "无肝素", nurse2);
        Patient p5 = patient("P0005", "陈志强", "男", 1980, RiskLevel.LOW, InfectionZone.HCV,
                false, null, dow, Shift.AFTERNOON, 75.0, 240, "普通肝素", nurse1);
        Patient p6 = patient("P0006", "杨丽娟", "女", 1975, RiskLevel.MEDIUM, InfectionZone.CLEAN,
                true, "06:40-07:10", dow, Shift.MORNING, 52.3, 240, "低分子肝素 4000IU", nurse2);
        Patient p7 = patient("P0007", "黄文斌", "男", 1968, RiskLevel.MEDIUM, InfectionZone.CLEAN,
                false, null, dow, Shift.AFTERNOON, 68.0, 240, "普通肝素", nurse1);
        Patient p8 = patient("P0008", "吴秀英", "女", 1955, RiskLevel.HIGH, InfectionZone.HBV,
                true, "12:20-12:50", dow, Shift.AFTERNOON, 46.8, 210, "无肝素", nurse2);
        // 非今日规律患者（用于看板"未排班"对照与明日演示）
        patient("P0009", "郑海涛", "男", 1977, RiskLevel.LOW, InfectionZone.CLEAN,
                false, null, nextDow(dow), Shift.MORNING, 80.1, 240, "普通肝素", nurse1);
        patient("P0010", "林玉梅", "女", 1963, RiskLevel.MEDIUM, InfectionZone.HCV,
                false, null, nextDow(dow), Shift.MORNING, 58.4, 240, "低分子肝素 3000IU", nurse2);
        patient("P0011", "许国平", "男", 1970, RiskLevel.MEDIUM, InfectionZone.CLEAN,
                false, null, nextDow(dow), Shift.EVENING, 72.0, 240, "普通肝素", nurse1);
        patient("P0012", "何春花", "女", 1958, RiskLevel.HIGH, InfectionZone.CLEAN,
                true, "18:00-18:30", nextDow(dow), Shift.EVENING, 50.5, 210, "无肝素", nurse2);

        // ---- 耗材 ----
        Consumable c1 = consumable("HC001", "空心纤维透析器", "支", 120, 40, false, "85.00");
        Consumable c2 = consumable("HC002", "体外循环血路管", "套", 110, 40, false, "42.00");
        Consumable c3 = consumable("HC003", "内瘘穿刺针", "支", 200, 60, false, "6.50");
        Consumable c4 = consumable("HC004", "低分子肝素注射液", "支", 90, 30, false, "28.00");
        Consumable c5 = consumable("HC005", "0.9%氯化钠注射液 500ml", "袋", 150, 50, false, "3.20");
        Consumable c6 = consumable("HC006", "高通量透析器（自费）", "支", 25, 10, true, "260.00");
        Consumable c7 = consumable("HC007", "一次性护理包（自费）", "包", 8, 15, true, "35.00"); // 低于安全库存
        consumable("HC008", "透析浓缩液A液", "桶", 60, 20, false, "48.00");
        consumable("HC009", "透析浓缩液B液", "桶", 55, 20, false, "36.00");

        // ---- 今日排班 ----
        // 已完成（早晨 M01 张国栋）
        Schedule s1 = schedule(p1, m01, today, Shift.MORNING, 1, ScheduleStatus.COMPLETED, headNurse);
        TreatmentRecord t1 = treatment(s1, nurse1, TreatmentStatus.COMPLETED,
                LocalDateTime.of(today, LocalTime.of(7, 5)), LocalDateTime.of(today, LocalTime.of(11, 10)));
        t1.setPreWeightKg(new BigDecimal("64.8")); t1.setPreBpSys(152); t1.setPreBpDia(88);
        t1.setAccessCondition("内瘘震颤正常"); t1.setLastDiscomfort("上次透析后轻度头晕");
        t1.setAnticoagPlan("低分子肝素 4000IU"); t1.setAdjustDehydration(true); t1.setPlannedUfMl(2300);
        t1.setPostWeightKg(new BigDecimal("62.6")); t1.setHemostasisOk(true);
        t1.setDepartureMethod(DepartureMethod.TRANSPORT); t1.setNextScheduleNote("按规律下次 " + dow + " 早班");
        treatmentRepo.save(t1);
        entry(t1, EntryType.VITALS, 148, 85, 76, "上机后首次测量", nurse1, today, 7, 30);
        entry(t1, EntryType.VITALS, 136, 80, 74, null, nurse1, today, 8, 30);
        entry(t1, EntryType.FLUID, null, null, null, "补液 0.9%氯化钠 250ml", nurse1, today, 9, 0);
        entry(t1, EntryType.VITALS, 128, 76, 72, null, nurse1, today, 9, 30);
        entry(t1, EntryType.VITALS, 124, 74, 70, "下机前测量", nurse1, today, 10, 50);
        // 已完成（早晨 M04 刘桂芳 HBV 区）
        Schedule s2 = schedule(p4, m07, today, Shift.MORNING, 1, ScheduleStatus.COMPLETED, headNurse);
        TreatmentRecord t2 = treatment(s2, nurse2, TreatmentStatus.COMPLETED,
                LocalDateTime.of(today, LocalTime.of(7, 10)), LocalDateTime.of(today, LocalTime.of(10, 45)));
        t2.setPreWeightKg(new BigDecimal("50.1")); t2.setPreBpSys(138); t2.setPreBpDia(82);
        t2.setAccessCondition("移植物内瘘正常"); t2.setAnticoagPlan("无肝素"); t2.setAdjustDehydration(false);
        t2.setPlannedUfMl(1500); t2.setPostWeightKg(new BigDecimal("48.7")); t2.setHemostasisOk(true);
        t2.setDepartureMethod(DepartureMethod.FAMILY);
        treatmentRepo.save(t2);
        entry(t2, EntryType.VITALS, 140, 84, 80, null, nurse2, today, 7, 40);
        entry(t2, EntryType.ALARM, null, null, null, "静脉压高报警，调整穿刺针位置后恢复", nurse2, today, 8, 20);
        entry(t2, EntryType.VITALS, 132, 78, 76, null, nurse2, today, 9, 40);

        // 透析中（早晨 M02 李秀兰）——含一次低血压
        Schedule s3 = schedule(p2, m02, today, Shift.MORNING, 1, ScheduleStatus.ON_DIALYSIS, headNurse);
        TreatmentRecord t3 = treatment(s3, nurse1, TreatmentStatus.ON_DIALYSIS,
                LocalDateTime.of(today, LocalTime.of(7, 15)), null);
        t3.setPreWeightKg(new BigDecimal("57.2")); t3.setPreBpSys(146); t3.setPreBpDia(86);
        t3.setAccessCondition("内瘘震颤正常"); t3.setLastDiscomfort("上次透析中曾低血压");
        t3.setAnticoagPlan("普通肝素"); t3.setAdjustDehydration(true); t3.setPlannedUfMl(2000);
        treatmentRepo.save(t3);
        m02.setStatus(MachineStatus.IN_USE); machineRepo.save(m02);
        entry(t3, EntryType.VITALS, 142, 84, 78, null, nurse1, today, 7, 45);
        entry(t3, EntryType.VITALS, 118, 70, 82, null, nurse1, today, 8, 45);
        MonitoringEntry hypo = entry(t3, EntryType.VITALS, 86, 54, 96, "患者出冷汗、恶心，暂停超滤并补液", nurse1, today, 9, 15);
        entry(t3, EntryType.DISCOMFORT, null, null, null, "恶心、出冷汗，头低足高位", nurse1, today, 9, 16);
        entry(t3, EntryType.FLUID, null, null, null, "快速补液 0.9%氯化钠 200ml", nurse1, today, 9, 18);
        exception(ExceptionType.HYPOTENSION, s3, p2, m02, t3, ExceptionStatus.ACKNOWLEDGED,
                "透析中低血压：收缩压 86 mmHg", nurse1, doctor, "已暂停超滤、补液，复测血压回升");
        // 已评估待上机（早晨 M03 杨丽娟）
        Schedule s4 = schedule(p6, m03, today, Shift.MORNING, 1, ScheduleStatus.PLANNED, headNurse);
        TreatmentRecord t4 = treatment(s4, nurse2, TreatmentStatus.PRE_RECORDED, null, null);
        t4.setPreWeightKg(new BigDecimal("54.0")); t4.setPreBpSys(150); t4.setPreBpDia(90);
        t4.setAccessCondition("内瘘震颤减弱，已通知医生"); t4.setAnticoagPlan("低分子肝素 4000IU");
        t4.setAdjustDehydration(false); t4.setPlannedUfMl(1800);
        treatmentRepo.save(t4);

        // 下午班计划（含预领耗材）
        Schedule s5 = schedule(p3, m01, today, Shift.AFTERNOON, 1, ScheduleStatus.PLANNED, headNurse);
        Schedule s6 = schedule(p5, m09, today, Shift.AFTERNOON, 1, ScheduleStatus.PLANNED, headNurse);
        Schedule s7 = schedule(p7, m05, today, Shift.AFTERNOON, 1, ScheduleStatus.PLANNED, headNurse);
        Schedule s8 = schedule(p8, m08, today, Shift.AFTERNOON, 1, ScheduleStatus.PLANNED, headNurse);
        preDraw(c1, s5, nurse2); preDraw(c2, s5, nurse2); preDraw(c3, s5, nurse2);
        preDraw(c6, s5, nurse2); // 自费高通量透析器
        preDraw(c1, s6, nurse2); preDraw(c2, s6, nurse2);

        // 昨日一条已完成治疗 + 已核对账单（供报表）
        LocalDate yesterday = today.minusDays(1);
        Schedule sy = schedule(p1, m01, yesterday, Shift.MORNING, 1, ScheduleStatus.COMPLETED, headNurse);
        TreatmentRecord ty = treatment(sy, nurse1, TreatmentStatus.COMPLETED,
                LocalDateTime.of(yesterday, LocalTime.of(7, 5)), LocalDateTime.of(yesterday, LocalTime.of(11, 5)));
        ty.setPreWeightKg(new BigDecimal("64.5")); ty.setPostWeightKg(new BigDecimal("62.5"));
        ty.setHemostasisOk(true); ty.setDepartureMethod(DepartureMethod.TRANSPORT);
        treatmentRepo.save(ty);

        // ---- 消毒/工程记录 ----
        disinfection(m01, DisinfectionType.DISINFECTION, engineer, today, 11, 40, "PASS", "热消毒+柠檬酸", null);
        disinfection(m07, DisinfectionType.DISINFECTION, engineer, today, 11, 0, "PASS", "化学消毒", null);
        disinfection(m01, DisinfectionType.FILTER_CHANGE, engineer, today.minusDays(2), 9, 0, "PASS",
                "更换A/B液滤芯", LocalDateTime.of(today.plusDays(28), LocalTime.of(9, 0)));
        disinfection(m01, DisinfectionType.WATER_TREATMENT, engineer, today, 5, 30, "PASS",
                "反渗水电导率 12μs/cm，余氯<0.1mg/L", null);
        disinfection(m02, DisinfectionType.WATER_TREATMENT, engineer, today, 5, 35, "PASS", "水处理巡检正常", null);

        // ---- 账单 ----
        BillingRecord b1 = new BillingRecord();
        b1.setTreatment(t1); b1.setInsuranceAmount(new BigDecimal("400.00"));
        b1.setSelfPayAmount(new BigDecimal("260.00")); b1.setSelfPayDetail("高通量透析器（自费）×1=260.00");
        billingRepo.save(b1);
        BillingRecord b2 = new BillingRecord();
        b2.setTreatment(t2); b2.setInsuranceAmount(new BigDecimal("400.00"));
        b2.setSelfPayAmount(BigDecimal.ZERO);
        billingRepo.save(b2);
        BillingRecord by = new BillingRecord();
        by.setTreatment(ty); by.setInsuranceAmount(new BigDecimal("400.00"));
        by.setSelfPayAmount(new BigDecimal("35.00")); by.setSelfPayDetail("一次性护理包（自费）×1=35.00");
        by.setStatus(BillingStatus.CONFIRMED); by.setConfirmedBy(finance);
        by.setConfirmedAt(LocalDateTime.of(yesterday, LocalTime.of(14, 30)));
        billingRepo.save(by);

        // ---- 其他异常 ----
        exception(ExceptionType.MACHINE_FAULT, null, null, m10, null, ExceptionStatus.OPEN,
                "机器 M10 故障：自检电导度报警，暂停排班", engineer, null, null);
        exception(ExceptionType.CONSUMABLE_SHORTAGE, null, null, null, null, ExceptionStatus.OPEN,
                "一次性护理包（自费） 库存 8 低于安全库存 15", null, null, null);
        exception(ExceptionType.LATE, s7, p7, m05, null, ExceptionStatus.RESOLVED,
                "患者迟到：交通车晚点 20 分钟", nurse2, headNurse, "已顺延至 13:30 上机");
        exception(ExceptionType.INFECTION_CHANGE, null, p5, null, null, ExceptionStatus.ACKNOWLEDGED,
                "患者陈志强 HCV 抗体复查待确认，暂维持 C 区透析", doctor, headNurse, "等待检验科复核结果");
    }

    private User user(String username, String password, String name, Role role) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(password));
        u.setName(name);
        u.setRole(role);
        return userRepo.save(u);
    }

    private Machine machine(String code, InfectionZone zone, String model, String location) {
        Machine m = new Machine();
        m.setCode(code); m.setZone(zone); m.setModel(model); m.setLocation(location);
        return machineRepo.save(m);
    }

    private Patient patient(String mrn, String name, String gender, int birthYear, RiskLevel risk,
                            InfectionZone zone, boolean transport, String window, String patternDays,
                            Shift shift, double dryWeight, int duration, String anticoag, User primaryNurse) {
        Patient p = new Patient();
        p.setMrn(mrn); p.setName(name); p.setGender(gender);
        p.setBirthDate(LocalDate.of(birthYear, 6, 15));
        p.setPhone("138****" + String.format("%04d", Math.abs(mrn.hashCode()) % 10000));
        p.setRiskLevel(risk); p.setInfectionZone(zone);
        p.setTransportNeeded(transport); p.setTransportWindow(window);
        p.setPatternDays(patternDays); p.setPreferredShift(shift);
        p.setDryWeightKg(BigDecimal.valueOf(dryWeight)); p.setUsualDurationMin(duration);
        p.setAnticoagPlan(anticoag); p.setPrimaryNurse(primaryNurse);
        return patientRepo.save(p);
    }

    private Consumable consumable(String code, String name, String unit, int stock, int safety,
                                  boolean selfPaid, String price) {
        Consumable c = new Consumable();
        c.setCode(code); c.setName(name); c.setUnit(unit); c.setStock(stock);
        c.setSafetyStock(safety); c.setSelfPaid(selfPaid); c.setUnitPrice(new BigDecimal(price));
        return consumableRepo.save(c);
    }

    private Schedule schedule(Patient p, Machine m, LocalDate date, Shift shift, int seq,
                              ScheduleStatus status, User creator) {
        Schedule s = new Schedule();
        s.setPatient(p); s.setMachine(m); s.setScheduleDate(date); s.setShift(shift);
        s.setSeqOrder(seq); s.setStatus(status);
        s.setPlannedDurationMin(p.getUsualDurationMin());
        s.setTransportSlot(p.isTransportNeeded() ? p.getTransportWindow() : null);
        s.setCreatedBy(creator);
        return scheduleRepo.save(s);
    }

    private TreatmentRecord treatment(Schedule s, User nurse, TreatmentStatus status,
                                      LocalDateTime start, LocalDateTime end) {
        TreatmentRecord t = new TreatmentRecord();
        t.setSchedule(s); t.setNurse(nurse); t.setStatus(status);
        t.setStartTime(start); t.setEndTime(end);
        if (status != TreatmentStatus.PRE_RECORDED || start != null) {
            t.setPreRecordedAt(start != null ? start.minusMinutes(20) : LocalDateTime.now().minusHours(1));
        } else {
            t.setPreRecordedAt(LocalDateTime.now().minusMinutes(30));
        }
        return treatmentRepo.save(t);
    }

    private MonitoringEntry entry(TreatmentRecord t, EntryType type, Integer sys, Integer dia, Integer hr,
                                  String note, User by, LocalDate date, int hour, int minute) {
        MonitoringEntry e = new MonitoringEntry();
        e.setTreatment(t); e.setType(type); e.setBpSys(sys); e.setBpDia(dia); e.setHeartRate(hr);
        e.setNote(note); e.setRecordedBy(by);
        e.setRecordedAt(LocalDateTime.of(date, LocalTime.of(hour, minute)));
        return monitoringRepo.save(e);
    }

    private void preDraw(Consumable c, Schedule s, User operator) {
        c.setStock(c.getStock() - 1);
        consumableRepo.save(c);
        ConsumableTxn txn = new ConsumableTxn();
        txn.setConsumable(c); txn.setSchedule(s); txn.setQuantity(1);
        txn.setType(TxnType.PRE_DRAW); txn.setOperator(operator); txn.setNote("排班预领");
        txnRepo.save(txn);
    }

    private void disinfection(Machine m, DisinfectionType type, User by, LocalDate date, int h, int min,
                              String result, String note, LocalDateTime nextDue) {
        DisinfectionRecord r = new DisinfectionRecord();
        r.setMachine(m); r.setType(type); r.setPerformedBy(by);
        r.setPerformedAt(LocalDateTime.of(date, LocalTime.of(h, min)));
        r.setResult(result); r.setNote(note); r.setNextDueAt(nextDue);
        disinfectionRepo.save(r);
    }

    private void exception(ExceptionType type, Schedule s, Patient p, Machine m, TreatmentRecord t,
                           ExceptionStatus status, String note, User creator, User handler, String handleNote) {
        ExceptionEvent e = new ExceptionEvent();
        e.setType(type); e.setSchedule(s); e.setPatient(p); e.setMachine(m); e.setTreatment(t);
        e.setStatus(status); e.setNote(note); e.setCreatedBy(creator);
        if (handler != null) {
            e.setHandledBy(handler); e.setHandledAt(LocalDateTime.now().minusMinutes(15));
            e.setHandleNote(handleNote);
        }
        exceptionRepo.save(e);
    }

    private String nextDow(String dow) {
        List<String> days = List.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN");
        return days.get((days.indexOf(dow) + 1) % 7);
    }

    /** 避免循环依赖：本地计算星期编码 */
    static class ScheduleServiceDayCode {
        static String of(LocalDate date) {
            return switch (date.getDayOfWeek()) {
                case MONDAY -> "MON"; case TUESDAY -> "TUE"; case WEDNESDAY -> "WED";
                case THURSDAY -> "THU"; case FRIDAY -> "FRI"; case SATURDAY -> "SAT";
                case SUNDAY -> "SUN";
            };
        }
    }
}
