package com.hdms.repo;

import com.hdms.model.Machine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MachineRepo extends JpaRepository<Machine, Long> {
    List<Machine> findByActiveTrueOrderByCode();
}
