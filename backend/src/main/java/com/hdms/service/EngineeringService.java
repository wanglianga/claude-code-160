package com.hdms.service;

import com.hdms.config.GlobalExceptionHandler.BizException;
import com.hdms.config.SecurityUtils;
import com.hdms.model.DisinfectionRecord;
import com.hdms.model.Machine;
import com.hdms.model.enums.DisinfectionType;
import com.hdms.model.enums.ExceptionType;
import com.hdms.model.enums.MachineStatus;
import com.hdms.repo.DisinfectionRepo;
import com.hdms.repo.MachineRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EngineeringService {
    private final MachineRepo machineRepo;
    private final DisinfectionRepo disinfectionRepo;
    private final ExceptionService exceptionService;

    public EngineeringService(MachineRepo machineRepo, DisinfectionRepo disinfectionRepo,
                              ExceptionService exceptionService) {
        this.machineRepo = machineRepo;
        this.disinfectionRepo = disinfectionRepo;
        this.exceptionService = exceptionService;
    }

    @Transactional(readOnly = true)
    public List<Machine> machines() {
        return machineRepo.findByActiveTrueOrderByCode();
    }

    /** 更新机器状态；报故障自动产生异常并提示受影响排班 */
    @Transactional
    public Machine updateStatus(Long machineId, MachineStatus status, String note) {
        Machine m = machineRepo.findById(machineId).orElseThrow(() -> new BizException("机器不存在"));
        m.setStatus(status);
        machineRepo.save(m);
        if (status == MachineStatus.FAULT) {
            exceptionService.create(ExceptionType.MACHINE_FAULT, null, null, m, null,
                    "机器 " + m.getCode() + " 故障：" + (note == null ? "" : note));
        }
        return m;
    }

    /** 登记消毒/滤芯更换/水处理记录；消毒合格后机器恢复空闲 */
    @Transactional
    public DisinfectionRecord addRecord(Long machineId, DisinfectionType type, String result,
                                        String note, LocalDateTime nextDueAt) {
        Machine m = machineRepo.findById(machineId).orElseThrow(() -> new BizException("机器不存在"));
        DisinfectionRecord r = new DisinfectionRecord();
        r.setMachine(m);
        r.setType(type);
        r.setResult(result == null ? "PASS" : result);
        r.setNote(note);
        r.setNextDueAt(nextDueAt);
        r.setPerformedBy(SecurityUtils.currentUser());
        disinfectionRepo.save(r);
        if (type == DisinfectionType.DISINFECTION && "PASS".equals(r.getResult())
                && m.getStatus() == MachineStatus.DISINFECTING) {
            m.setStatus(MachineStatus.IDLE);
            machineRepo.save(m);
        }
        return r;
    }

    @Transactional(readOnly = true)
    public List<DisinfectionRecord> recentRecords() {
        return disinfectionRepo.findTop50ByOrderByPerformedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<DisinfectionRecord> recordsOfMachine(Long machineId) {
        return disinfectionRepo.findByMachineIdOrderByPerformedAtDesc(machineId);
    }
}
