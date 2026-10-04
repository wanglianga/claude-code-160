package com.hd.center.repository;

import com.hd.center.entity.DialysisMachine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MachineRepository extends JpaRepository<DialysisMachine, Long> {
    List<DialysisMachine> findByActiveTrueOrderByCode();
}
