package com.hd.center.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 透析中定时记录：血压、心率、报警、补液、抽血、不适、医嘱变更。
 */
@Data
@Entity
@Table(name = "intra_record", indexes = @Index(name = "idx_intra_session", columnList = "sessionId"))
public class IntraRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sessionId;

    @Column(nullable = false)
    private LocalDateTime recordTime = LocalDateTime.now();

    private Integer bpSys;

    private Integer bpDia;

    private Integer heartRate;

    /** 机器报警内容 */
    @Column(length = 255)
    private String alarm;

    /** 补液量 ml */
    private Integer fluidMl;

    /** 抽血/化验 */
    @Column(length = 255)
    private String bloodDraw;

    /** 患者不适 */
    @Column(length = 500)
    private String discomfort;

    /** 医嘱变更 */
    @Column(length = 500)
    private String orderChange;

    @Column(length = 64)
    private String nurseName;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
