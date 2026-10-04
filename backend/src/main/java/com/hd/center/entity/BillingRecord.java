package com.hd.center.entity;

import com.hd.center.entity.Enums.BillingStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收费核对单：医保、自费耗材、补收费用。
 */
@Data
@Entity
@Table(name = "billing_record")
public class BillingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long sessionId;

    @Column(nullable = false, length = 64)
    private String patientName;

    /** 医保结算金额 */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal insuranceAmount = BigDecimal.ZERO;

    /** 自费耗材金额 */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal selfPayAmount = BigDecimal.ZERO;

    /** 补收费用 */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal extraCharge = BigDecimal.ZERO;

    @Column(length = 500)
    private String extraReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BillingStatus status = BillingStatus.PENDING;

    @Column(length = 64)
    private String confirmedBy;

    private LocalDateTime confirmedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
