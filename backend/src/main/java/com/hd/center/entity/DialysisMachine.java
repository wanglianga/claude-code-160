package com.hd.center.entity;

import com.hd.center.entity.Enums.MachineStatus;
import com.hd.center.entity.Enums.MachineZone;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 透析机/机位。
 */
@Data
@Entity
@Table(name = "dialysis_machine")
public class DialysisMachine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 机位号，如 M01 */
    @Column(nullable = false, unique = true, length = 16)
    private String code;

    @Column(length = 64)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MachineZone zone = MachineZone.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MachineStatus status = MachineStatus.IDLE;

    private LocalDateTime lastDisinfectedAt;

    /** 两台治疗之间所需消毒间隔（分钟） */
    @Column(nullable = false)
    private Integer disinfectBufferMin = 40;

    @Column(nullable = false)
    private Boolean active = true;
}
