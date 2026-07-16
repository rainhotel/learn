package dev.notifyflow.course.providerstub;

import java.time.Duration;

import dev.notifyflow.course.providerstub.ProviderStubService.DeliveryOutcome;
import dev.notifyflow.course.providerstub.ProviderStubService.DeliveryRequest;
import dev.notifyflow.course.providerstub.ProviderStubService.EffectsView;
import dev.notifyflow.course.providerstub.ProviderStubService.ScenarioView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class ProviderStubController {

    private final ProviderStubService service;

    public ProviderStubController(ProviderStubService service) {
        this.service = service;
    }

    @PostMapping("/provider/v1/deliveries")
    public ResponseEntity<DeliveryOutcome> deliver(
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody DeliveryHttpRequest request) {
        DeliveryOutcome outcome = service.deliver(
                idempotencyKey,
                new DeliveryRequest(request.recipientRef(), request.templateCode(), request.variables()));
        HttpStatus status = outcome.status() == ProviderDeliveryStatus.REJECTED
                ? HttpStatus.UNPROCESSABLE_ENTITY
                : HttpStatus.OK;
        return ResponseEntity.status(status).body(outcome);
    }

    @GetMapping("/provider/v1/deliveries/by-idempotency-key/{key}")
    public DeliveryOutcome query(@PathVariable("key") String idempotencyKey) {
        return service.query(idempotencyKey);
    }

    @PutMapping("/internal/course/v1/scenarios/{key}")
    public ScenarioView configureScenario(
            @PathVariable("key") String idempotencyKey,
            @Valid @RequestBody ScenarioRequest request) {
        Duration delay = request.delayMillis() == null ? null : Duration.ofMillis(request.delayMillis());
        return service.configureScenario(idempotencyKey, request.scenario(), delay);
    }

    @GetMapping("/internal/course/v1/effects/{key}")
    public EffectsView effects(@PathVariable("key") String idempotencyKey) {
        return service.effects(idempotencyKey);
    }

    @DeleteMapping("/internal/course/v1/scenarios")
    public ResponseEntity<Void> clearScenarios() {
        service.clearScenarios();
        return ResponseEntity.noContent().build();
    }

    public record DeliveryHttpRequest(
            @NotBlank String recipientRef,
            @NotBlank String templateCode,
            @NotNull java.util.Map<String, Object> variables) {
    }

    public record ScenarioRequest(
            @NotNull ProviderScenario scenario,
            @Positive Long delayMillis) {
    }
}
