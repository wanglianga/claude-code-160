package com.hdms.controller;

import com.hdms.model.DisinfectionRecord;
import com.hdms.model.Machine;
import com.hdms.model.enums.DisinfectionType;
import com.hdms.model.enums.MachineStatus;
import com.hdms.service.EngineeringService;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/engineering")
public class EngineeringController {
    private final EngineeringService engineeringService;

    public EngineeringController(EngineeringService engineeringService) {
        this.engineeringService = engineeringService;
    }

    @GetMapping("/machines")
    public List<Machine> machines() {
        return engineeringService.machines();
    }

    public record StatusReq(@NotNull MachineStatus status, String note) {}

    @PutMapping("/machines/{id}/status")
    @PreAuthorize("hasAnyRole('ENGINEER','ADMIN','HEAD_NURSE')")
    public Machine updateStatus(@PathVariable Long id, @RequestBody StatusReq req) {
        return engineeringService.updateStatus(id, req.status(), req.note());
    }

    public record RecordReq(@NotNull Long machineId, @NotNull DisinfectionType type,
                            String result, String note, LocalDateTime nextDueAt) {}

    @PostMapping("/records")
    @PreAuthorize("hasAnyRole('ENGINEER','ADMIN')")
    public DisinfectionRecord addRecord(@RequestBody RecordReq req) {
        return engineeringService.addRecord(req.machineId(), req.type(), req.result(), req.note(), req.nextDueAt());
    }

    @GetMapping("/records")
    public List<DisinfectionRecord> recent() {
        return engineeringService.recentRecords();
    }

    @GetMapping("/records/machine/{id}")
    public List<DisinfectionRecord> ofMachine(@PathVariable Long id) {
        return engineeringService.recordsOfMachine(id);
    }
}
