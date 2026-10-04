package com.hdms.model;

import com.hdms.model.enums.InfectionZone;
import com.hdms.model.enums.RiskLevel;
import com.hdms.model.enums.Shift;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "patients")
@Getter @Setter
public class Patient {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 病历号 */
    @Column(unique = true, nullable = false, length = 30)
    private String mrn;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 10)
    private String gender;

    private LocalDate birthDate;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RiskLevel riskLevel = RiskLevel.MEDIUM;

    /** 传染病分区（决定可上机的机器分区） */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private InfectionZone infectionZone = InfectionZone.CLEAN;

    /** 是否需要交通接送 */
    private boolean transportNeeded = false;

    /** 接送时间窗，如 06:30-07:00 */
    @Column(length = 30)
    private String transportWindow;

    /** 固定透析日，如 MON,WED,FRI */
    @Column(length = 30)
    private String patternDays;

    /** 固定班次 */
    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private Shift preferredShift;

    /** 干体重 kg */
    private BigDecimal dryWeightKg;

    /** 常规透析时长（分钟） */
    private Integer usualDurationMin = 240;

    /** 常规抗凝方案 */
    @Column(length = 100)
    private String anticoagPlan;

    /** 熟悉该患者的护士（排班参考） */
    @ManyToOne(fetch = FetchType.EAGER)
    private User primaryNurse;

    private boolean active = true;

    @Column(length = 500)
    private String notes;
}
