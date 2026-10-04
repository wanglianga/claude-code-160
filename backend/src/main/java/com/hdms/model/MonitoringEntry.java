package com.hdms.model;

import com.hdms.model.enums.EntryType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 透析中定时记录：血压/心率/报警/补液/抽血/不适/医嘱变更 */
@Entity
@Table(name = "monitoring_entries")
@Getter @Setter
public class MonitoringEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private TreatmentRecord treatment;

    private LocalDateTime recordedAt = LocalDateTime.now();

    private Integer bpSys;
    private Integer bpDia;
    private Integer heartRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EntryType type = EntryType.VITALS;

    @Column(length = 500)
    private String note;

    @ManyToOne(fetch = FetchType.EAGER)
    private User recordedBy;
}
