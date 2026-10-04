package com.hdms.controller;

import com.hdms.model.Patient;
import com.hdms.repo.PatientRepo;
import com.hdms.config.GlobalExceptionHandler.BizException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {
    private final PatientRepo patientRepo;

    public PatientController(PatientRepo patientRepo) {
        this.patientRepo = patientRepo;
    }

    @GetMapping
    public List<Patient> list() {
        return patientRepo.findByActiveTrueOrderByMrn();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HEAD_NURSE','DOCTOR')")
    public Patient create(@RequestBody Patient patient) {
        patient.setId(null);
        return patientRepo.save(patient);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HEAD_NURSE','DOCTOR')")
    public Patient update(@PathVariable Long id, @RequestBody Patient body) {
        Patient p = patientRepo.findById(id).orElseThrow(() -> new BizException("患者不存在"));
        p.setName(body.getName());
        p.setGender(body.getGender());
        p.setBirthDate(body.getBirthDate());
        p.setPhone(body.getPhone());
        p.setRiskLevel(body.getRiskLevel());
        // 感染标记变化会产生异常事件，由前端调用异常接口联动
        p.setInfectionZone(body.getInfectionZone());
        p.setTransportNeeded(body.isTransportNeeded());
        p.setTransportWindow(body.getTransportWindow());
        p.setPatternDays(body.getPatternDays());
        p.setPreferredShift(body.getPreferredShift());
        p.setDryWeightKg(body.getDryWeightKg());
        p.setUsualDurationMin(body.getUsualDurationMin());
        p.setAnticoagPlan(body.getAnticoagPlan());
        p.setNotes(body.getNotes());
        return patientRepo.save(p);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HEAD_NURSE')")
    public void deactivate(@PathVariable Long id) {
        Patient p = patientRepo.findById(id).orElseThrow(() -> new BizException("患者不存在"));
        p.setActive(false);
        patientRepo.save(p);
    }
}
