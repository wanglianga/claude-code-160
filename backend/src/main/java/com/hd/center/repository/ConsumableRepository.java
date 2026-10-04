package com.hd.center.repository;

import com.hd.center.entity.Consumable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsumableRepository extends JpaRepository<Consumable, Long> {
    List<Consumable> findAllByOrderByCode();
}
