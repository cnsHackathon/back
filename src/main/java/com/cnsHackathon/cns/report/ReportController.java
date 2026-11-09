package com.cnsHackathon.cns.report;

import com.cnsHackathon.cns.report.dto.ReportResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions/{sessionId}")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/report")
    public ReportResponse report(@PathVariable("sessionId") String sessionId) {
        return reportService.getReport(sessionId);
    }
}
