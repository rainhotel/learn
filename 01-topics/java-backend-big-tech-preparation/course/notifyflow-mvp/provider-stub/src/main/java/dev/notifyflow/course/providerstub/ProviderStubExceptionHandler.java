package dev.notifyflow.course.providerstub;

import java.util.Map;

import dev.notifyflow.course.providerstub.ProviderStubService.ScenarioAlreadyAppliedException;
import jakarta.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ProviderStubExceptionHandler {

    @ExceptionHandler(ScenarioAlreadyAppliedException.class)
    ResponseEntity<Map<String, String>> scenarioAlreadyApplied(ScenarioAlreadyAppliedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("code", "SCENARIO_ALREADY_APPLIED", "message", exception.getMessage()));
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            ConstraintViolationException.class,
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class
    })
    ResponseEntity<Map<String, String>> badRequest(Exception exception) {
        return ResponseEntity.badRequest()
                .body(Map.of("code", "INVALID_REQUEST", "message", exception.getMessage()));
    }
}
