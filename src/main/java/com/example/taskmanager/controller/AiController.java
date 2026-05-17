package com.example.taskmanager.controller;

import com.example.taskmanager.domain.dto.TaskDtos.*;
import com.example.taskmanager.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    /**
     * POST /api/ai/suggest
     *
     * Accepts a free-text prompt and returns AI-generated task suggestions.
     * Example body: { "prompt": "I need to plan a product launch" }
     */
    @PostMapping("/suggest")
    public ResponseEntity<AiSuggestResponse> suggest(@Valid @RequestBody AiSuggestRequest request) {
        String suggestion = aiService.getSuggestion(request.prompt());
        return ResponseEntity.ok(new AiSuggestResponse(suggestion));
    }
}
