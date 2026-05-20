package com.example.taskmanager.exception;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.taskmanager.service.AiService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorResponse(
            int status,
            String message,
            Map<String, String> errors,
            LocalDateTime timestamp
    ) {
        static ErrorResponse of(HttpStatus httpStatus, String message) {
            return new ErrorResponse(httpStatus.value(), message, null, LocalDateTime.now());
        }

        static ErrorResponse ofValidation(Map<String, String> fieldErrors) {
            return new ErrorResponse(400, "Validation failed", fieldErrors, LocalDateTime.now());
        }
    }

    // ── Task not found ────────────────────────────────────────────────────────

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(TaskNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    // ── Validation errors (@Valid) ────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (first, second) -> first
                ));

        return ResponseEntity.badRequest()
                .body(ErrorResponse.ofValidation(fieldErrors));
    }

    // ── Missing or malformed JSON body ────────────────────────────────────────

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMissingBody(HttpMessageNotReadableException ex) {
        log.warn("Request body missing or unreadable: {}", ex.getMessage());

        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST,
                        "Request body is missing or malformed"
                ));
    }

    // ── AI / upstream failures (AiServiceException) ───────────────────────────

    @ExceptionHandler(AiService.AiServiceException.class)
    public ResponseEntity<ErrorResponse> handleAiServiceException(AiService.AiServiceException ex) {
        log.error("AI service error (technical): {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ErrorResponse.of(
                        HttpStatus.BAD_GATEWAY,
                        ex.getUserMessage()   // safe user-facing message, never the technical detail
                ));
    }

    // ── AI / upstream failures (IllegalStateException) ────────────────────────

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        log.error("Upstream service error: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ErrorResponse.of(
                        HttpStatus.BAD_GATEWAY,
                        "AI service failed: " + ex.getMessage()
                ));
    }

    // ── Database errors ───────────────────────────────────────────────────────

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.error("Database constraint violation", ex);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST,
                        "The request could not be saved due to invalid or missing data."
                ));
    }

    // ── Catch-all (must stay LAST) ────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "An unexpected error has occurred"
                ));
    }
}