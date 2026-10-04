package com.hdms.repo;

import com.hdms.model.Consumable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsumableRepo extends JpaRepository<Consumable, Long> {
    List<Consumable> findAllByOrderByCode();
}
