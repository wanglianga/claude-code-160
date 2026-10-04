package com.hdms.model;

import com.hdms.model.enums.DisinfectionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 工程记录：透析机消毒 / 滤芯更换 / 水处理 */
@Entity
@Table(name = "disinfection_records")
@Getter @Setter
public class DisinfectionRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Machine machine;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DisinfectionType type;

    @ManyToOne(fetch = FetchType.EAGER)
    private User performedBy;

    private LocalDateTime performedAt = LocalDateTime.now();

    /** PASS / FAIL */
    @Column(length = 10)
    private String result = "PASS";

    @Column(length = 300)
    private String note;

    /** 下次到期时间（滤芯/水处理） */
    private LocalDateTime nextDueAt;
}
