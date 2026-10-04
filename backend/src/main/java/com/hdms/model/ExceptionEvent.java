package com.hdms.model;

import com.hdms.model.enums.ExceptionStatus;
import com.hdms.model.enums.ExceptionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 异常事件：迟到/请假/感染标记变化/机器故障/耗材不足/低血压/医嘱调整 */
@Entity
@Table(name = "exception_events")
@Getter @Setter
public class ExceptionEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private ExceptionType type;

    @ManyToOne(fetch = FetchType.EAGER)
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.EAGER)
    private Patient patient;

    @ManyToOne(fetch = FetchType.EAGER)
    private Machine machine;

    @ManyToOne(fetch = FetchType.EAGER)
    private TreatmentRecord treatment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private ExceptionStatus status = ExceptionStatus.OPEN;

    @Column(length = 500)
    private String note;

    @ManyToOne(fetch = FetchType.EAGER)
    private User createdBy;

    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.EAGER)
    private User handledBy;

    private LocalDateTime handledAt;

    @Column(length = 500)
    private String handleNote;
}
