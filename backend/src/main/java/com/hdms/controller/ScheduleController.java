package com.hdms.controller;

import com.hdms.model.Schedule;
import com.hdms.model.enums.Shift;
import com.hdms.service.ScheduleService;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {
    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping("/board")
    public Map<String, Object> board(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return scheduleService.board(date);
    }

    public record CreateReq(@NotNull Long patientId, @NotNull Long machineId, @NotNull LocalDate date,
                            @NotNull Shift shift, Integer durationMin, String note) {}

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HEAD_NURSE')")
    public Schedule create(@RequestBody CreateReq req) {
        return scheduleService.create(req.patientId(), req.machineId(), req.date(), req.shift(),
                req.durationMin(), req.note());
    }

    public record AutoReq(@NotNull LocalDate date, @NotNull Shift shift) {}

    @PostMapping("/auto")
    @PreAuthorize("hasAnyRole('ADMIN','HEAD_NURSE')")
    public Map<String, Object> auto(@RequestBody AutoReq req) {
        return scheduleService.autoArrange(req.date(), req.shift());
    }

    public record MoveReq(@NotNull Long machineId, @NotNull Shift shift) {}

    @PostMapping("/{id}/move/preview")
    @PreAuthorize("hasAnyRole('ADMIN','HEAD_NURSE')")
    public Map<String, Object> movePreview(@PathVariable Long id, @RequestBody MoveReq req) {
        return scheduleService.movePreview(id, req.machineId(), req.shift());
    }

    @PostMapping("/{id}/move")
    @PreAuthorize("hasAnyRole('ADMIN','HEAD_NURSE')")
    public Schedule move(@PathVariable Long id, @RequestBody MoveReq req) {
        return scheduleService.move(id, req.machineId(), req.shift());
    }

    public record ReasonReq(String reason) {}

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','HEAD_NURSE')")
    public Schedule cancel(@PathVariable Long id, @RequestBody(required = false) ReasonReq req) {
        return scheduleService.cancel(id, req == null ? null : req.reason());
    }

    @PostMapping("/{id}/absent")
    @PreAuthorize("hasAnyRole('ADMIN','HEAD_NURSE','NURSE')")
    public Schedule absent(@PathVariable Long id, @RequestBody(required = false) ReasonReq req) {
        return scheduleService.markAbsent(id, req == null ? null : req.reason());
    }

    @PostMapping("/{id}/late")
    @PreAuthorize("hasAnyRole('ADMIN','HEAD_NURSE','NURSE')")
    public void late(@PathVariable Long id, @RequestBody(required = false) ReasonReq req) {
        scheduleService.markLate(id, req == null ? null : req.reason());
    }
}
