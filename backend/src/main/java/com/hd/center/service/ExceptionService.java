package com.hd.center.service;

import com.hd.center.entity.*;
import com.hd.center.entity.Enums.ExceptionType;
import com.hd.center.repository.ExceptionEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 异常事件统一入口：各业务环节（排班、透中记录、机器状态、耗材）在此汇聚。
 */
@Service
@RequiredArgsConstructor
public class ExceptionService {

    private final ExceptionEventRepository exceptionRepo;

    @Transactional(propagation = Propagation.REQUIRED)
    public ExceptionEvent raise(ExceptionType type, Patient patient, DialysisSession session,
                                DialysisMachine machine, String description, String createdBy) {
        ExceptionEvent event = new ExceptionEvent();
        event.setType(type);
        if (patient != null) {
            event.setPatientId(patient.getId());
            event.setPatientName(patient.getName());
        }
        if (session != null) {
            event.setSessionId(session.getId());
            if (event.getPatientId() == null && session.getPatient() != null) {
                event.setPatientId(session.getPatient().getId());
                event.setPatientName(session.getPatient().getName());
            }
        }
        if (machine != null) {
            event.setMachineId(machine.getId());
            event.setMachineCode(machine.getCode());
        }
        event.setDescription(description);
        event.setCreatedBy(createdBy);
        return exceptionRepo.save(event);
    }
}
