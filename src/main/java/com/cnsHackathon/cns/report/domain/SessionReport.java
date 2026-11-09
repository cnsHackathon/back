package com.cnsHackathon.cns.report.domain;

import com.cnsHackathon.cns.report.ReportResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

/** 한 번 만든 리포트. 다시 요청하면 AI를 부르지 않고 저장본을 돌려준다. */
@Entity
@Table(name = "session_report")
public class SessionReport {

    @Id
    @Column(length = 36)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportResult result;

    @Column(nullable = false)
    private int score;

    /** ReportResponse JSON */
    @Lob
    @Column(nullable = false)
    private String payload;

    @Column(nullable = false)
    private Instant createdAt;

    protected SessionReport() {
    }

    public SessionReport(String sessionId, ReportResult result, int score, String payload, Instant createdAt) {
        this.sessionId = sessionId;
        this.result = result;
        this.score = score;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public String getPayload() {
        return payload;
    }
}
