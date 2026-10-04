package com.hdms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "consumables")
@Getter @Setter
public class Consumable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 10)
    private String unit;

    private Integer stock = 0;

    /** 安全库存，低于则触发耗材不足异常 */
    private Integer safetyStock = 0;

    /** 是否自费耗材 */
    private boolean selfPaid = false;

    private BigDecimal unitPrice = BigDecimal.ZERO;
}
