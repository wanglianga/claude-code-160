package com.hdms.service;

import com.hdms.config.GlobalExceptionHandler.BizException;
import com.hdms.config.SecurityUtils;
import com.hdms.model.*;
import com.hdms.model.enums.ExceptionStatus;
import com.hdms.model.enums.ExceptionType;
import com.hdms.repo.ExceptionRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExceptionService {
    private final ExceptionRepo exceptionRepo;

    public ExceptionService(ExceptionRepo exceptionRepo) {
        this.exceptionRepo = exceptionRepo;
    }

    /** 各业务模块共用的异常上报入口 */
    @Transactional(propagation = Propagation.REQUIRED)
    public ExceptionEvent create(ExceptionType type, Schedule schedule, Patient patient,
                                 Machine machine, TreatmentRecord treatment, String note) {
        ExceptionEvent e = new ExceptionEvent();
        e.setType(type);
        e.setSchedule(schedule);
        e.setPatient(patient);
        e.setMachine(machine);
        e.setTreatment(treatment);
        e.setNote(note);
        try {
            e.setCreatedBy(SecurityUtils.currentUser());
        } catch (Exception ignored) {
            // 系统内部触发时无登录用户
        }
        return exceptionRepo.save(e);
    }

    @Transactional(readOnly = true)
    public List<ExceptionEvent> list(ExceptionStatus status) {
        return status == null ? exceptionRepo.findTop200ByOrderByCreatedAtDesc()
                : exceptionRepo.findByStatusOrderByCreatedAtDesc(status);
    }

    @Transactional
    public ExceptionEvent handle(Long id, ExceptionStatus target, String handleNote) {
        ExceptionEvent e = exceptionRepo.findById(id).orElseThrow(() -> new BizException("异常不存在"));
        if (target != ExceptionStatus.ACKNOWLEDGED && target != ExceptionStatus.RESOLVED) {
            throw new BizException("非法状态");
        }
        e.setStatus(target);
        e.setHandledBy(SecurityUtils.currentUser());
        e.setHandledAt(LocalDateTime.now());
        e.setHandleNote(handleNote);
        return exceptionRepo.save(e);
    }
}
