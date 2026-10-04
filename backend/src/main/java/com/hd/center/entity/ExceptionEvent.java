package com.hd.center.entity;

import com.hd.center.entity.Enums.ExceptionStatus;
import com.hd.center.entity.Enums.ExceptionType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 异常事件：迟到、请假、感染标记变化、机器故障、耗材不足、透析中低血压、医嘱调整等。
 * 将患者、护士、医生、工程、收费的状态串联到一条治疗记录上。
 */
@Data
@Entity
@Table(name = "exception_event", indexes = @Index(name = "idx_exc_session", columnList = "sessionId"))
public class ExceptionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ExceptionType type;

    private Long patientId;

    @Column(length = 64)
    private String patientName;

    private Long sessionId;

    private Long machineId;

    @Column(length = 64)
    private String machineCode;

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ExceptionStatus status = ExceptionStatus.OPEN;

    @Column(length = 64)
    private String createdBy;

    @Column(length = 64)
    private String handlerName;

    @Column(length = 500)
    private String handleNote;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime resolvedAt;
}
