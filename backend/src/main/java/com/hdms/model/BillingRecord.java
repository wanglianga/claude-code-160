package com.hdms.model;

import com.hdms.model.enums.BillingStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 收费记录：医保 + 自费耗材 + 补收费用 */
@Entity
@Table(name = "billing_records")
@Getter @Setter
public class BillingRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(unique = true)
    private TreatmentRecord treatment;

    /** 医保结算金额 */
    private BigDecimal insuranceAmount = BigDecimal.ZERO;

    /** 自费耗材金额 */
    private BigDecimal selfPayAmount = BigDecimal.ZERO;

    /** 补收费用 */
    private BigDecimal extraAmount = BigDecimal.ZERO;

    @Column(length = 300)
    private String extraNote;

    /** 自费耗材明细（文本） */
    @Column(length = 500)
    private String selfPayDetail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private BillingStatus status = BillingStatus.PENDING;

    @ManyToOne(fetch = FetchType.EAGER)
    private User confirmedBy;

    private LocalDateTime confirmedAt;

    private LocalDateTime createdAt = LocalDateTime.now();
}
