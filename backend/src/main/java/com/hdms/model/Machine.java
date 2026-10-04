package com.hdms.model;

import com.hdms.model.enums.InfectionZone;
import com.hdms.model.enums.MachineStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "machines")
@Getter @Setter
public class Machine {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private InfectionZone zone = InfectionZone.CLEAN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private MachineStatus status = MachineStatus.IDLE;

    @Column(length = 50)
    private String model;

    @Column(length = 50)
    private String location;

    private boolean active = true;
}
