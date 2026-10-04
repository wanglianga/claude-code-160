package com.hd.center.entity;

import com.hd.center.entity.Enums.AccessType;
import com.hd.center.entity.Enums.InfectionMarker;
import com.hd.center.entity.Enums.RiskLevel;
import com.hd.center.entity.Enums.Shift;
import com.hd.center.entity.Enums.TransportMode;
import jakarta.persistence.*;
import lombok.Data;

/**
 * 血透患者档案：固定每周透析规律 + 感染标记 + 风险 + 接送 + 抗凝方案。
 */
@Data
@Entity
@Table(name = "patient")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 病历号 */
    @Column(nullable = false, unique = true, length = 32)
    private String mrn;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 8)
    private String gender;

    private Integer age;

    @Column(length = 32)
    private String phone;

    /** 感染标记：决定可上机的分区 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InfectionMarker infectionMarker = InfectionMarker.NONE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RiskLevel riskLevel = RiskLevel.LOW;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private AccessType accessType = AccessType.AVF;

    /** 干体重 kg */
    private Double dryWeight;

    /** 抗凝方案 */
    @Column(length = 255)
    private String anticoagPlan;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private TransportMode transportMode = TransportMode.SELF;

    /** 接送时间窗，如 06:50-07:10 */
    @Column(length = 32)
    private String transportWindow;

    /** 固定透析日，逗号分隔的星期数字（1=周一 ... 7=周日），如 "1,3,5" */
    @Column(length = 32)
    private String usualPattern;

    @Enumerated(EnumType.STRING)
    @Column(length = 8)
    private Shift usualShift = Shift.AM;

    /** 单次透析时长（分钟） */
    private Integer durationMin = 240;

    /** 熟悉该患者的护士 */
    @Column(length = 64)
    private String familiarNurse;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(length = 500)
    private String notes;
}
