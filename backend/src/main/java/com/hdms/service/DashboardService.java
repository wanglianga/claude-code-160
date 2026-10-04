package com.hdms.service;

import com.hdms.model.Machine;
import com.hdms.model.enums.ExceptionStatus;
import com.hdms.model.enums.MachineStatus;
import com.hdms.model.enums.ScheduleStatus;
import com.hdms.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    private final ScheduleRepo scheduleRepo;
    private final MachineRepo machineRepo;
    private final ExceptionRepo exceptionRepo;
    private final ConsumableRepo consumableRepo;
    private final BillingRepo billingRepo;

    public DashboardService(ScheduleRepo scheduleRepo, MachineRepo machineRepo, ExceptionRepo exceptionRepo,
                            ConsumableRepo consumableRepo, BillingRepo billingRepo) {
        this.scheduleRepo = scheduleRepo;
        this.machineRepo = machineRepo;
        this.exceptionRepo = exceptionRepo;
        this.consumableRepo = consumableRepo;
        this.billingRepo = billingRepo;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> today() {
        LocalDate today = LocalDate.now();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("date", today.toString());
        m.put("scheduled", scheduleRepo.countByScheduleDate(today));
        m.put("onDialysis", scheduleRepo.countByScheduleDateAndStatus(today, ScheduleStatus.ON_DIALYSIS));
        m.put("completed", scheduleRepo.countByScheduleDateAndStatus(today, ScheduleStatus.COMPLETED));
        m.put("planned", scheduleRepo.countByScheduleDateAndStatus(today, ScheduleStatus.PLANNED));
        m.put("absent", scheduleRepo.countByScheduleDateAndStatus(today, ScheduleStatus.ABSENT));
        m.put("openExceptions", exceptionRepo.countByStatus(ExceptionStatus.OPEN));
        m.put("pendingBilling", billingRepo.findByStatusOrderByCreatedAtDesc(
                com.hdms.model.enums.BillingStatus.PENDING).size());
        m.put("machineStatus", machineRepo.findByActiveTrueOrderByCode().stream()
                .collect(Collectors.groupingBy(mm -> mm.getStatus().name(), Collectors.counting())));
        m.put("lowStock", consumableRepo.findAllByOrderByCode().stream()
                .filter(c -> c.getStock() < c.getSafetyStock())
                .map(c -> Map.of("name", c.getName(), "stock", c.getStock(), "safetyStock", c.getSafetyStock()))
                .toList());
        return m;
    }
}
