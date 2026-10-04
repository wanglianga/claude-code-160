package com.hdms.controller;

import com.hdms.model.BillingRecord;
import com.hdms.model.enums.BillingStatus;
import com.hdms.service.BillingService;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/billing")
@PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
public class BillingController {
    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping
    public List<BillingRecord> list(@RequestParam(required = false) BillingStatus status) {
        return billingService.list(status);
    }

    public record AdjustReq(@NotNull BigDecimal extraAmount, String extraNote) {}

    @PostMapping("/{id}/adjust")
    public BillingRecord adjust(@PathVariable Long id, @RequestBody AdjustReq req) {
        return billingService.adjust(id, req.extraAmount(), req.extraNote());
    }

    @PostMapping("/{id}/confirm")
    public BillingRecord confirm(@PathVariable Long id) {
        return billingService.confirm(id);
    }
}
