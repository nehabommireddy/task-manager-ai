package com.example.taskmanager.controller;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.taskmanager.domain.dto.AiDtos.AiSuggestRequest;
import com.example.taskmanager.domain.dto.AiDtos.AiSuggestResponse;
import com.example.taskmanager.service.AiService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    /**
     * POST /api/tasks/suggest
     *
     * Accepts a free-text goal and returns structured task suggestions.
     */
    @PostMapping("/suggest")
        public ResponseEntity<AiSuggestResponse> suggest(
                @RequestBody @Valid AiSuggestRequest request
        ) {
            return ResponseEntity.ok(aiService.getSuggestions(request.prompt()));
        }
}

