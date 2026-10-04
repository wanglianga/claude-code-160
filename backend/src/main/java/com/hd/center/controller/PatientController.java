package com.hd.center.controller;

import com.hd.center.config.CurrentUser;
import com.hd.center.entity.DialysisSession;
import com.hd.center.entity.Enums.ExceptionType;
import com.hd.center.entity.Enums.SessionStatus;
import com.hd.center.entity.Patient;
import com.hd.center.repository.PatientRepository;
import com.hd.center.repository.SessionRepository;
import com.hd.center.service.ExceptionService;
import com.hd.center.service.SchedulingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientRepository patientRepo;
    private final SessionRepository sessionRepo;
    private final ExceptionService exceptionService;

    @GetMapping
    public List<Patient> list(@RequestParam(required = false) String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            return patientRepo.findByNameContainingOrMrnContainingOrderByMrn(keyword.trim(), keyword.trim());
        }
        return patientRepo.findAll();
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        Patient patient = get(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("patient", patient);
        result.put("sessions", sessionRepo.findByPatientIdOrderBySessionDateDesc(id).stream().limit(20).toList());
        return result;
    }

    @PostMapping
    public Patient create(@RequestBody Patient patient) {
        patient.setId(null);
        return patientRepo.save(patient);
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody Patient body,
                                      @AuthenticationPrincipal CurrentUser current) {
        Patient patient = get(id);
        String oldMarker = patient.getInfectionMarker().name();

        patient.setName(body.getName());
        patient.setGender(body.getGender());
        patient.setAge(body.getAge());
        patient.setPhone(body.getPhone());
        patient.setInfectionMarker(body.getInfectionMarker());
        patient.setRiskLevel(body.getRiskLevel());
        patient.setAccessType(body.getAccessType());
        patient.setDryWeight(body.getDryWeight());
        patient.setAnticoagPlan(body.getAnticoagPlan());
        patient.setTransportMode(body.getTransportMode());
        patient.setTransportWindow(body.getTransportWindow());
        patient.setUsualPattern(body.getUsualPattern());
        patient.setUsualShift(body.getUsualShift());
        patient.setDurationMin(body.getDurationMin());
        patient.setFamiliarNurse(body.getFamiliarNurse());
        patient.setActive(body.getActive());
        patient.setNotes(body.getNotes());
        patientRepo.save(patient);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("patient", patient);

        // 感染标记变化：上报异常并检查未来排班的分区匹配
        if (!oldMarker.equals(patient.getInfectionMarker().name())) {
            exceptionService.raise(ExceptionType.INFECTION_CHANGE, patient, null, null,
                    "患者「" + patient.getName() + "」感染标记由 " + oldMarker + " 变更为 "
                            + patient.getInfectionMarker() + "，请核对后续排班分区", current.name());
            List<DialysisSession> future = sessionRepo.findByPatientIdOrderBySessionDateDesc(id).stream()
                    .filter(s -> !s.getSessionDate().isBefore(LocalDate.now()))
                    .filter(s -> s.getStatus() == SessionStatus.SCHEDULED || s.getStatus() == SessionStatus.ASSESSED)
                    .filter(s -> !SchedulingService.compatibleZones(patient.getInfectionMarker())
                            .contains(s.getMachine().getZone()))
                    .toList();
            if (!future.isEmpty()) {
                result.put("zoneMismatchSessions", future);
                result.put("warning", "有 " + future.size() + " 条未来排班与新感染分区不匹配，请调班");
            }
        }
        return result;
    }

    private Patient get(Long id) {
        return patientRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "患者不存在"));
    }
}
