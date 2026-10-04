package com.hdms.controller;

import com.hdms.model.ExceptionEvent;
import com.hdms.model.enums.ExceptionStatus;
import com.hdms.model.enums.ExceptionType;
import com.hdms.repo.MachineRepo;
import com.hdms.repo.PatientRepo;
import com.hdms.repo.ScheduleRepo;
import com.hdms.service.ExceptionService;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exceptions")
public class ExceptionController {
    private final ExceptionService exceptionService;
    private final ScheduleRepo scheduleRepo;
    private final PatientRepo patientRepo;
    private final MachineRepo machineRepo;

    public ExceptionController(ExceptionService exceptionService, ScheduleRepo scheduleRepo,
                               PatientRepo patientRepo, MachineRepo machineRepo) {
        this.exceptionService = exceptionService;
        this.scheduleRepo = scheduleRepo;
        this.patientRepo = patientRepo;
        this.machineRepo = machineRepo;
    }

    @GetMapping
    public List<ExceptionEvent> list(@RequestParam(required = false) ExceptionStatus status) {
        return exceptionService.list(status);
    }

    public record CreateReq(@NotNull ExceptionType type, Long scheduleId, Long patientId, Long machineId,
                            String note) {}

    @PostMapping
    public ExceptionEvent create(@RequestBody CreateReq req) {
        return exceptionService.create(req.type(),
                req.scheduleId() == null ? null : scheduleRepo.findById(req.scheduleId()).orElse(null),
                req.patientId() == null ? null : patientRepo.findById(req.patientId()).orElse(null),
                req.machineId() == null ? null : machineRepo.findById(req.machineId()).orElse(null),
                null, req.note());
    }

    public record HandleReq(@NotNull ExceptionStatus status, String handleNote) {}

    @PostMapping("/{id}/handle")
    @PreAuthorize("hasAnyRole('ADMIN','HEAD_NURSE','DOCTOR','ENGINEER','NURSE')")
    public ExceptionEvent handle(@PathVariable Long id, @RequestBody HandleReq req) {
        return exceptionService.handle(id, req.status(), req.handleNote());
    }
}
