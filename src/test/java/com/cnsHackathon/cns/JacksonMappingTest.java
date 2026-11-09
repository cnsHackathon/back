package com.cnsHackathon.cns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cnsHackathon.cns.conversation.ScammerReply;
import com.cnsHackathon.cns.conversation.dto.TurnRequest;
import com.cnsHackathon.cns.report.Evaluation;
import com.cnsHackathon.cns.report.ReportResult;
import com.cnsHackathon.cns.report.dto.ReportResponse;
import com.cnsHackathon.cns.scenario.ScamType;
import com.cnsHackathon.cns.scenario.ScenarioCatalog;
import com.cnsHackathon.cns.session.dto.CreateSessionRequest;
import com.cnsHackathon.cns.session.profile.AgeGroup;
import com.cnsHackathon.cns.session.profile.ChildAgeGroup;
import com.cnsHackathon.cns.session.profile.Job;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Jackson 3로 실제 JSON을 읽어 본다. Jackson 3는 빠진 int/boolean을 오류로 처리하는 등
 * Jackson 2와 기본 동작이 달라서, 서버를 띄우기 전에 여기서 걸러 낸다.
 */
class JacksonMappingTest {

    private final ObjectMapper mapper = JsonMapper.builder().build();

    @Test
    void 시나리오_JSON을_모두_읽고_검사한다() {
        ScenarioCatalog catalog = new ScenarioCatalog(mapper);

        assertFalse(catalog.all().isEmpty());
    }

    @Test
    void 프론트가_보내는_요청을_읽는다() {
        CreateSessionRequest request = mapper.readValue("""
                {"ageGroup":"70대 이상","gender":"FEMALE","job":"무직·은퇴",
                 "children":[{"gender":"MALE","ageGroup":"성인"}]}
                """, CreateSessionRequest.class);
        TurnRequest turn = mapper.readValue("{\"userText\":\"네\",\"isFinal\":true}", TurnRequest.class);

        assertEquals(AgeGroup.SEVENTIES_PLUS, request.ageGroup());
        assertEquals(Job.UNEMPLOYED_OR_RETIRED, request.job());
        assertEquals(ChildAgeGroup.ADULT, request.toProfile().children().get(0).ageGroup());
        assertTrue(turn.finalRequested());
    }

    @Test
    void AI_응답에서_빠진_값은_기본값으로_채운다() {
        ScammerReply reply = mapper.readValue("""
                {"speaker":"C","utterance":"여보세요","phase":"APPROACH","endReason":"NONE"}
                """, ScammerReply.class);
        Evaluation evaluation = mapper.readValue("""
                {"result":"SAFE","summary":"요약","checkpoints":[{"id":"DOUBT"}],"riskyMoments":[{"why":"이유"}]}
                """, Evaluation.class);

        assertFalse(reply.ended());
        assertEquals(0, evaluation.scammedTurn());
        assertFalse(evaluation.checkpoints().get(0).achieved());
        assertEquals(0, evaluation.riskyMoments().get(0).turn());
    }

    @Test
    void 저장한_리포트를_다시_읽는다() {
        ReportResponse report = new ReportResponse("s", 72, ReportResult.SAFE, "요약", null, null,
                List.of(new ReportResponse.Moment(1, "누구세요?", "의심했어요.")), List.of(), List.of("팁"),
                List.of(ScamType.LOAN),
                List.of(new ReportResponse.CheckpointResult("DOUBT", "의심", 100, true, 1)),
                new ReportResponse.ScenarioInfo("loan-1", ScamType.LOAN, "대출 빙자", "제목"));

        ReportResponse restored = mapper.readValue(mapper.writeValueAsString(report), ReportResponse.class);

        assertEquals(report, restored);
    }
}
