package com.hdms.model;

import com.hdms.model.enums.ScheduleStatus;
import com.hdms.model.enums.Shift;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "schedules",
        uniqueConstraints = @UniqueConstraint(columnNames = {"patient_id", "scheduleDate", "shift"}))
@Getter @Setter
public class Schedule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Machine machine;

    @Column(nullable = false)
    private LocalDate scheduleDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Shift shift;

    /** 上机顺序（同机同班次内） */
    private Integer seqOrder = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private ScheduleStatus status = ScheduleStatus.PLANNED;

    /** 计划透析时长（分钟） */
    private Integer plannedDurationMin = 240;

    @Column(length = 30)
    private String transportSlot;

    @Column(length = 300)
    private String note;

    @Column(length = 300)
    private String cancelReason;

    @ManyToOne(fetch = FetchType.EAGER)
    private User createdBy;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Version
    private long version;
}
