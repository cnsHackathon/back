package com.cnsHackathon.cns.session;

import com.cnsHackathon.cns.session.dto.CreateSessionRequest;
import com.cnsHackathon.cns.session.dto.CreateSessionResponse;
import com.cnsHackathon.cns.session.dto.EndSessionRequest;
import com.cnsHackathon.cns.session.dto.EndSessionResponse;
import com.cnsHackathon.cns.session.dto.StartSessionResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public CreateSessionResponse create(@Valid @RequestBody CreateSessionRequest request) {
        return sessionService.create(request);
    }

    @PostMapping("/{sessionId}/start")
    public StartSessionResponse start(@PathVariable("sessionId") String sessionId) {
        return sessionService.start(sessionId);
    }

    @PostMapping("/{sessionId}/end")
    public EndSessionResponse end(@PathVariable("sessionId") String sessionId,
                                  @Valid @RequestBody EndSessionRequest request) {
        return sessionService.end(sessionId, request);
    }
}
