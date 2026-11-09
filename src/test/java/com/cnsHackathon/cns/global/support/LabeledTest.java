package com.cnsHackathon.cns.global.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cnsHackathon.cns.session.profile.AgeGroup;
import com.cnsHackathon.cns.session.profile.ChildAgeGroup;
import com.cnsHackathon.cns.session.profile.Gender;
import com.cnsHackathon.cns.session.profile.Job;
import org.junit.jupiter.api.Test;

class LabeledTest {

    @Test
    void 프론트가_보내는_한글_값을_읽는다() {
        assertEquals(AgeGroup.SEVENTIES_PLUS, AgeGroup.from("70대 이상"));
        assertEquals(Job.UNEMPLOYED_OR_RETIRED, Job.from("무직·은퇴"));
        assertEquals(ChildAgeGroup.SECONDARY, ChildAgeGroup.from("중고등학생"));
    }

    @Test
    void enum_이름도_읽는다() {
        assertEquals(Gender.FEMALE, Gender.from("FEMALE"));
        assertEquals(Job.SELF_EMPLOYED, Job.from("self_employed"));
    }

    @Test
    void 없는_값은_거절한다() {
        assertThrows(IllegalArgumentException.class, () -> AgeGroup.from("80대"));
    }
}
