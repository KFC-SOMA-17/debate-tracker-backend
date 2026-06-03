package com.debatetracker.debate.controller.debate;

import com.debatetracker.debate.domain.debate.Debate;
import com.debatetracker.debate.service.debate.DebateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DebateRestController {

    private final DebateService debateService;

    @PostMapping("/api/debates")
    public ResponseEntity<DebateCreateResponse> createDebate(@RequestBody @Valid DebateCreateRequest request) {
        Debate debate = debateService.create(request.toDomain());
        return ResponseEntity.ok(new DebateCreateResponse(debate));
    }
}
