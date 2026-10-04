package com.hdms.controller;

import com.hdms.model.MonitoringEntry;
import com.hdms.model.TreatmentRecord;
import com.hdms.model.enums.DepartureMethod;
import com.hdms.model.enums.EntryType;
import com.hdms.service.TreatmentService;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/treatments")
public class TreatmentController {
    private final TreatmentService treatmentService;

    public TreatmentController(TreatmentService treatmentService) {
        this.treatmentService = treatmentService;
    }

    @GetMapping
    public List<TreatmentRecord> ofDate(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return treatmentService.ofDate(date);
    }

    @GetMapping("/{id}")
    public Map<String, Object> timeline(@PathVariable Long id) {
        return treatmentService.timeline(id);
    }

    public record PreReq(@NotNull Long scheduleId, BigDecimal preWeightKg, Integer preBpSys, Integer preBpDia,
                         String accessCondition, String lastDiscomfort, String anticoagPlan,
                         Boolean adjustDehydration, Integer plannedUfMl, String preNotes) {}

    @PostMapping("/pre")
    @PreAuthorize("hasAnyRole('NURSE','HEAD_NURSE','ADMIN')")
    public TreatmentRecord pre(@RequestBody PreReq req) {
        return treatmentService.preAssess(req.scheduleId(), req.preWeightKg(), req.preBpSys(), req.preBpDia(),
                req.accessCondition(), req.lastDiscomfort(), req.anticoagPlan(),
                req.adjustDehydration(), req.plannedUfMl(), req.preNotes());
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('NURSE','HEAD_NURSE','ADMIN')")
    public TreatmentRecord start(@PathVariable Long id) {
        return treatmentService.start(id);
    }

    public record EntryReq(@NotNull EntryType type, Integer bpSys, Integer bpDia, Integer heartRate, String note) {}

    @PostMapping("/{id}/entries")
    @PreAuthorize("hasAnyRole('NURSE','HEAD_NURSE','ADMIN','DOCTOR')")
    public MonitoringEntry addEntry(@PathVariable Long id, @RequestBody EntryReq req) {
        return treatmentService.addEntry(id, req.type(), req.bpSys(), req.bpDia(), req.heartRate(), req.note());
    }

    public record CompleteReq(BigDecimal postWeightKg, @NotNull Boolean hemostasisOk,
                              DepartureMethod departureMethod, String nextScheduleNote, String postNotes) {}

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('NURSE','HEAD_NURSE','ADMIN')")
    public TreatmentRecord complete(@PathVariable Long id, @RequestBody CompleteReq req) {
        return treatmentService.complete(id, req.postWeightKg(), req.hemostasisOk(),
                req.departureMethod(), req.nextScheduleNote(), req.postNotes());
    }
}
