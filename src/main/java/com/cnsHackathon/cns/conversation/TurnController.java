package com.cnsHackathon.cns.conversation;

import com.cnsHackathon.cns.conversation.dto.TurnRequest;
import com.cnsHackathon.cns.conversation.dto.TurnResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions/{sessionId}")
public class TurnController {

    private final ConversationService conversationService;

    public TurnController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping("/turns")
    public TurnResponse reply(@PathVariable("sessionId") String sessionId,
                              @Valid @RequestBody TurnRequest request) {
        return conversationService.reply(sessionId, request);
    }
}
