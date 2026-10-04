package com.hdms.controller;

import com.hdms.model.Consumable;
import com.hdms.model.ConsumableTxn;
import com.hdms.model.Schedule;
import com.hdms.model.enums.TxnType;
import com.hdms.repo.ScheduleRepo;
import com.hdms.service.ConsumableService;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/consumables")
public class ConsumableController {
    private final ConsumableService consumableService;
    private final ScheduleRepo scheduleRepo;

    public ConsumableController(ConsumableService consumableService, ScheduleRepo scheduleRepo) {
        this.consumableService = consumableService;
        this.scheduleRepo = scheduleRepo;
    }

    @GetMapping
    public List<Consumable> list() {
        return consumableService.list();
    }

    @GetMapping("/txns")
    public List<ConsumableTxn> recentTxns() {
        return consumableService.recentTxns();
    }

    @GetMapping("/txns/schedule/{scheduleId}")
    public List<ConsumableTxn> ofSchedule(@PathVariable Long scheduleId) {
        return consumableService.txnsOfSchedule(scheduleId);
    }

    public record TxnReq(@NotNull Long consumableId, Long scheduleId, @NotNull TxnType type,
                         @NotNull Integer quantity, String note) {}

    @PostMapping("/txns")
    @PreAuthorize("hasAnyRole('NURSE','HEAD_NURSE','ADMIN','ENGINEER')")
    public ConsumableTxn transact(@RequestBody TxnReq req) {
        Schedule schedule = req.scheduleId() == null ? null
                : scheduleRepo.findById(req.scheduleId()).orElse(null);
        return consumableService.transact(req.consumableId(), req.scheduleId(), req.type(),
                req.quantity(), req.note(), schedule);
    }
}
