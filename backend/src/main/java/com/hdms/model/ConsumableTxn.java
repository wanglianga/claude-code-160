package com.hdms.model;

import com.hdms.model.enums.TxnType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "consumable_txns")
@Getter @Setter
public class ConsumableTxn {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Consumable consumable;

    /** 关联排班（预领/使用时） */
    @ManyToOne(fetch = FetchType.EAGER)
    private Schedule schedule;

    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TxnType type;

    @ManyToOne(fetch = FetchType.EAGER)
    private User operator;

    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(length = 300)
    private String note;
}
