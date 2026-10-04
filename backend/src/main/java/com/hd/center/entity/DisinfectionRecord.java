package com.hd.center.entity;

import com.hd.center.entity.Enums.DisinfectionResult;
import com.hd.center.entity.Enums.DisinfectionType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工程记录：透析机消毒、滤芯更换、水处理。
 */
@Data
@Entity
@Table(name = "disinfection_record", indexes = @Index(name = "idx_disinf_machine", columnList = "machineId,doneAt"))
public class DisinfectionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 水处理等房间级记录可不绑定具体机器 */
    private Long machineId;

    @Column(length = 16)
    private String machineCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DisinfectionType type;

    @Column(length = 64)
    private String operatorName;

    @Column(nullable = false)
    private LocalDateTime doneAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private DisinfectionResult result = DisinfectionResult.PASS;

    @Column(length = 500)
    private String note;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
