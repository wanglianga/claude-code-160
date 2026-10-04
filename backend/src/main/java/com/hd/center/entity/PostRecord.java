package com.hd.center.entity;

import com.hd.center.entity.Enums.DepartureMode;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 透析后（下机）记录：下机体重、穿刺点止血、离开方式、下次排班备注。
 */
@Data
@Entity
@Table(name = "post_record")
public class PostRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long sessionId;

    /** 下机体重 kg */
    private Double offWeight;

    /** 穿刺点止血是否完成 */
    private Boolean hemostasisOk = true;

    /** 止血用时（分钟） */
    private Integer hemostasisMin;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private DepartureMode departureMode = DepartureMode.SELF;

    /** 下次排班备注 */
    @Column(length = 500)
    private String nextScheduleNote;

    @Column(length = 64)
    private String nurseName;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
