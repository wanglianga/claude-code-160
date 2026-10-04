package com.hd.center.entity;

import com.hd.center.entity.Enums.AccessCondition;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 透析前评估：体重、血压、通路、上次不适、抗凝方案、是否调整脱水量。
 */
@Data
@Entity
@Table(name = "pre_assessment")
public class PreAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long sessionId;

    /** 透前体重 kg */
    private Double weight;

    private Integer bpSys;

    private Integer bpDia;

    private Integer heartRate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AccessCondition accessCondition = AccessCondition.NORMAL;

    /** 上次透析后不适 */
    @Column(length = 500)
    private String lastDiscomfort;

    /** 本次抗凝方案 */
    @Column(length = 255)
    private String anticoagPlan;

    /** 是否需要调整脱水量 */
    private Boolean adjustDehydration = false;

    /** 目标脱水量 L */
    private Double dehydrationTarget;

    @Column(length = 500)
    private String note;

    @Column(length = 64)
    private String nurseName;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
