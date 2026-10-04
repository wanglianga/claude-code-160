package com.hd.center.controller;

import com.hd.center.config.CurrentUser;
import com.hd.center.entity.BillingRecord;
import com.hd.center.entity.DialysisSession;
import com.hd.center.entity.Enums.BillingStatus;
import com.hd.center.entity.Enums.SessionStatus;
import com.hd.center.repository.BillingRecordRepository;
import com.hd.center.repository.SessionRepository;
import com.hd.center.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingRecordRepository billingRepo;
    private final SessionRepository sessionRepo;
    private final BillingService billingService;

    @GetMapping
    public List<BillingRecord> list(@RequestParam(required = false) BillingStatus status) {
        if (status != null) {
            return billingRepo.findByStatusOrderByCreatedAtDesc(status);
        }
        return billingRepo.findTop100ByOrderByCreatedAtDesc();
    }

    /** 调整补收费用/自费金额（核对前） */
    @PutMapping("/{id}")
    public BillingRecord update(@PathVariable Long id, @RequestBody BillingRecord body) {
        BillingRecord record = get(id);
        if (record.getStatus() == BillingStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "已核对的账单不能修改");
        }
        if (body.getInsuranceAmount() != null) record.setInsuranceAmount(body.getInsuranceAmount());
        if (body.getSelfPayAmount() != null) record.setSelfPayAmount(body.getSelfPayAmount());
        if (body.getExtraCharge() != null) record.setExtraCharge(body.getExtraCharge());
        if (body.getExtraReason() != null) record.setExtraReason(body.getExtraReason());
        return billingRepo.save(record);
    }

    /** 财务核对确认：治疗单随之完成 */
    @PostMapping("/{id}/confirm")
    public BillingRecord confirm(@PathVariable Long id, @AuthenticationPrincipal CurrentUser current) {
        BillingRecord record = billingService.confirm(id, current.name());
        sessionRepo.findById(record.getSessionId()).ifPresent(s -> {
            if (s.getStatus() == SessionStatus.OFF_DIALYSIS) {
                s.setStatus(SessionStatus.FINISHED);
                sessionRepo.save(s);
            }
        });
        return record;
    }

    private BillingRecord get(Long id) {
        return billingRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "账单不存在"));
    }
}
