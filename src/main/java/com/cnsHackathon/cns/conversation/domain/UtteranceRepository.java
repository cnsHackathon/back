package com.cnsHackathon.cns.conversation.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UtteranceRepository extends JpaRepository<Utterance, Long> {

    List<Utterance> findBySessionIdOrderBySeqAsc(String sessionId);
}
