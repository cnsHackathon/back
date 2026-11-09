package com.cnsHackathon.cns.scenario;

import com.cnsHackathon.cns.session.profile.ChildAgeGroup;
import com.cnsHackathon.cns.session.profile.Job;
import com.cnsHackathon.cns.session.profile.Profile;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * resources/scenarios/*.json 한 개. 실제 사례를 조사해 만든 사기 시나리오의 뼈대이고,
 * AI는 여기에 사용자 프로필을 입혀 연기한다.
 *
 * @param personas    등장인물. 프론트가 C, B 두 목소리만 구분하므로 최대 2명이고 첫 번째는 C다.
 * @param flow        사기범이 밟아 갈 단계
 * @param demand      사기범의 최종 요구
 * @param redFlags    사용자가 알아챌 수 있는 위험 신호 (평가에 쓴다)
 * @param checkpoints 잘 대처했는지 보는 기준. weight 합은 100
 * @param tips        실전 대응 수칙. 평가 AI가 참고하고, 비어 있는 리포트를 채울 때 쓴다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Scenario(
        String id,
        ScamType subType,
        String title,
        Eligibility eligibility,
        List<Persona> personas,
        List<String> flow,
        String demand,
        List<String> redFlags,
        List<Checkpoint> checkpoints,
        List<String> tips,
        List<Source> sources) {

    public Scenario {
        eligibility = eligibility == null ? Eligibility.OPEN : eligibility;
        personas = personas == null ? List.of() : List.copyOf(personas);
        flow = flow == null ? List.of() : List.copyOf(flow);
        redFlags = redFlags == null ? List.of() : List.copyOf(redFlags);
        checkpoints = checkpoints == null ? List.of() : List.copyOf(checkpoints);
        tips = tips == null ? List.of() : List.copyOf(tips);
        sources = sources == null ? List.of() : List.copyOf(sources);
    }

    /** 등장인물 키를 순서대로. 예: [C, B] */
    public Set<String> speakerKeys() {
        Set<String> keys = new LinkedHashSet<>();
        personas.forEach(persona -> keys.add(persona.key()));
        return keys;
    }

    /**
     * 이 시나리오가 나올 수 있는 프로필 조건. 비어 있는 조건은 따지지 않는다.
     *
     * @param jobs            이 직업일 때만 (예: 노쇼 사기는 자영업자)
     * @param requiresMarried 기혼일 때만 (예: 배우자 사고 빙자)
     * @param childAgeGroups  이 연령대 자녀가 한 명 이상 있을 때만
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Eligibility(List<Job> jobs, Boolean requiresMarried, List<ChildAgeGroup> childAgeGroups) {

        static final Eligibility OPEN = new Eligibility(List.of(), false, List.of());

        // JSON에서 빠진 항목은 조건 없음으로 본다. (Jackson 3는 빠진 boolean을 오류로 처리해서 Boolean으로 받는다)
        public Eligibility {
            jobs = jobs == null ? List.of() : List.copyOf(jobs);
            requiresMarried = Boolean.TRUE.equals(requiresMarried);
            childAgeGroups = childAgeGroups == null ? List.of() : List.copyOf(childAgeGroups);
        }

        public boolean allows(Profile profile) {
            if (!jobs.isEmpty() && !jobs.contains(profile.job())) {
                return false;
            }
            if (requiresMarried && !profile.isMarried()) {
                return false;
            }
            return childAgeGroups.isEmpty() || profile.hasChildIn(childAgeGroups);
        }

        public boolean isOpenToAll() {
            return jobs.isEmpty() && !requiresMarried && childAgeGroups.isEmpty();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Persona(String key, String role) {
    }

    /** weight가 빠지면 ScenarioValidator가 서버 시작 때 알려 준다. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Checkpoint(String id, String desc, Integer weight) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Source(String title, String url) {
    }
}
