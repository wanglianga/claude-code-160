package com.hd.center.entity;

import com.hd.center.entity.Enums.SessionStatus;
import com.hd.center.entity.Enums.Shift;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 透析治疗单（排班 + 治疗过程 + 费用的主线记录）。
 */
@Data
@Entity
@Table(name = "dialysis_session",
        indexes = {
                @Index(name = "idx_session_date_shift", columnList = "sessionDate,shift"),
                @Index(name = "idx_session_machine", columnList = "machine_id,sessionDate")
        })
public class DialysisSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne(optional = false)
    @JoinColumn(name = "machine_id")
    private DialysisMachine machine;

    @Column(nullable = false)
    private LocalDate sessionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Shift shift;

    /** 上机顺序（同一班次内） */
    private Integer seqNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status = SessionStatus.SCHEDULED;

    private LocalDateTime plannedStart;

    @Column(nullable = false)
    private Integer plannedDurationMin = 240;

    private LocalDateTime actualStartAt;

    private LocalDateTime actualEndAt;

    /** 责任护士 */
    @Column(length = 64)
    private String nurseName;

    @Column(length = 500)
    private String note;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
