package com.hdms.controller;

import com.hdms.service.DashboardService;
import com.hdms.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StatsController {
    private final DashboardService dashboardService;
    private final ReportService reportService;

    public StatsController(DashboardService dashboardService, ReportService reportService) {
        this.dashboardService = dashboardService;
        this.reportService = reportService;
    }

    @GetMapping("/dashboard/today")
    public Map<String, Object> today() {
        return dashboardService.today();
    }

    @GetMapping("/reports/overview")
    @PreAuthorize("hasAnyRole('ADMIN','HEAD_NURSE')")
    public Map<String, Object> overview(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.overview(from, to);
    }
}
