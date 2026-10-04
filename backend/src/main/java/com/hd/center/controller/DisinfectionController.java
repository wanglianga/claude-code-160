package com.hd.center.controller;

import com.hd.center.config.CurrentUser;
import com.hd.center.entity.DialysisMachine;
import com.hd.center.entity.DisinfectionRecord;
import com.hd.center.entity.Enums.DisinfectionResult;
import com.hd.center.entity.Enums.DisinfectionType;
import com.hd.center.entity.Enums.MachineStatus;
import com.hd.center.repository.DisinfectionRecordRepository;
import com.hd.center.repository.MachineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工程记录：透析机消毒、滤芯更换、水处理。
 */
@RestController
@RequestMapping("/api/disinfections")
@RequiredArgsConstructor
public class DisinfectionController {

    private final DisinfectionRecordRepository disinfectionRepo;
    private final MachineRepository machineRepo;

    @GetMapping
    public List<DisinfectionRecord> list(@RequestParam(required = false) Long machineId,
                                         @RequestParam(required = false)
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (machineId != null) {
            return disinfectionRepo.findByMachineIdOrderByDoneAtDesc(machineId);
        }
        if (date != null) {
            return disinfectionRepo.findByDoneAtBetween(date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        }
        return disinfectionRepo.findTop50ByOrderByDoneAtDesc();
    }

    /**
     * 登记工程记录。消毒合格后将“消毒中”的机器置回空闲。
     */
    @PostMapping
    public DisinfectionRecord create(@RequestBody DisinfectionRecord body,
                                     @AuthenticationPrincipal CurrentUser current) {
        body.setId(null);
        body.setOperatorName(current.name());
        if (body.getDoneAt() == null) {
            body.setDoneAt(LocalDateTime.now());
        }
        if (body.getMachineId() != null) {
            DialysisMachine machine = machineRepo.findById(body.getMachineId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "机位不存在"));
            body.setMachineCode(machine.getCode());
            boolean isDisinfection = body.getType() == DisinfectionType.DAILY
                    || body.getType() == DisinfectionType.HEAT
                    || body.getType() == DisinfectionType.CITRIC_ACID;
            if (isDisinfection && body.getResult() == DisinfectionResult.PASS) {
                machine.setLastDisinfectedAt(body.getDoneAt());
                if (machine.getStatus() == MachineStatus.DISINFECTING) {
                    machine.setStatus(MachineStatus.IDLE);
                }
                machineRepo.save(machine);
            }
        }
        return disinfectionRepo.save(body);
    }
}
