package com.hdms.model;

import com.hdms.model.enums.DepartureMethod;
import com.hdms.model.enums.TreatmentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 治疗记录：把患者、护士、医生、工程、收费窗口的状态连成一条记录。
 */
@Entity
@Table(name = "treatment_records")
@Getter @Setter
public class TreatmentRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(unique = true)
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.EAGER)
    private User nurse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TreatmentStatus status = TreatmentStatus.PRE_RECORDED;

    // ---- 透析前 ----
    private BigDecimal preWeightKg;
    private Integer preBpSys;
    private Integer preBpDia;
    /** 通路情况：内瘘震颤正常 / 减弱 / 红肿 / 渗血 ... */
    @Column(length = 100)
    private String accessCondition;
    /** 上次透析不适 */
    @Column(length = 300)
    private String lastDiscomfort;
    /** 抗凝方案 */
    @Column(length = 100)
    private String anticoagPlan;
    /** 是否调整脱水量 */
    private Boolean adjustDehydration = false;
    /** 计划脱水量 ml */
    private Integer plannedUfMl;
    @Column(length = 300)
    private String preNotes;
    private LocalDateTime preRecordedAt;

    // ---- 透析中 ----
    private LocalDateTime startTime;

    // ---- 透析后 ----
    private LocalDateTime endTime;
    private BigDecimal postWeightKg;
    /** 穿刺点止血确认 */
    private Boolean hemostasisOk;
    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private DepartureMethod departureMethod;
    /** 下次排班说明 */
    @Column(length = 300)
    private String nextScheduleNote;
    @Column(length = 300)
    private String postNotes;
}
