package com.cnsHackathon.cns.session.domain;

import com.cnsHackathon.cns.session.profile.AgeGroup;
import com.cnsHackathon.cns.session.profile.Gender;
import com.cnsHackathon.cns.session.profile.Job;
import com.cnsHackathon.cns.session.profile.Profile;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 모의 통화 한 건. 로그인이 없으므로 이 id가 사용자를 대신한다. */
@Entity
@Table(name = "call_session")
public class CallSession {

    @Id
    @Column(length = 36)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AgeGroup ageGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Job job;

    private Boolean married;

    @Column(length = 50)
    private String region;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "call_session_child", joinColumns = @JoinColumn(name = "session_id"))
    private List<ChildInfo> children = new ArrayList<>();

    @Column(nullable = false, length = 100)
    private String scenarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EndReason endReason;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant startedAt;

    private Instant deadlineAt;

    private Instant endedAt;

    @Version
    private Long version;

    protected CallSession() {
    }

    public static CallSession create(Profile profile, String scenarioId, Instant now) {
        CallSession session = new CallSession();
        session.id = UUID.randomUUID().toString();
        session.ageGroup = profile.ageGroup();
        session.gender = profile.gender();
        session.job = profile.job();
        session.married = profile.married();
        session.region = profile.region();
        profile.children().forEach(child -> session.children.add(ChildInfo.from(child)));
        session.scenarioId = scenarioId;
        session.status = SessionStatus.CREATED;
        session.createdAt = now;
        return session;
    }

    public Profile profile() {
        return new Profile(ageGroup, gender, job, married, children.stream().map(ChildInfo::toChild).toList(), region);
    }

    /** 받기를 누른 순간부터 시간을 잰다. 여러 번 불러도 처음 시각을 유지한다. */
    public void start(Instant now, Duration timeLimit) {
        if (startedAt != null) {
            return;
        }
        startedAt = now;
        deadlineAt = now.plus(timeLimit);
        status = SessionStatus.IN_PROGRESS;
    }

    /** 이미 끝난 통화면 처음 종료 사유를 유지한다. */
    public void end(EndReason reason, Instant now) {
        if (isEnded()) {
            return;
        }
        status = SessionStatus.ENDED;
        endReason = reason;
        endedAt = now;
    }

    public boolean isStarted() {
        return startedAt != null;
    }

    public boolean isEnded() {
        return status == SessionStatus.ENDED;
    }

    public boolean isOverdue(Instant now) {
        return deadlineAt != null && !now.isBefore(deadlineAt);
    }

    public Duration elapsed(Instant now) {
        if (startedAt == null || now.isBefore(startedAt)) {
            return Duration.ZERO;
        }
        return Duration.between(startedAt, now);
    }

    public Duration remaining(Instant now, Duration timeLimit) {
        if (deadlineAt == null) {
            return timeLimit;
        }
        Duration remaining = Duration.between(now, deadlineAt);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    public String getId() {
        return id;
    }

    public AgeGroup getAgeGroup() {
        return ageGroup;
    }

    public String getScenarioId() {
        return scenarioId;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public EndReason getEndReason() {
        return endReason;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getDeadlineAt() {
        return deadlineAt;
    }
}
