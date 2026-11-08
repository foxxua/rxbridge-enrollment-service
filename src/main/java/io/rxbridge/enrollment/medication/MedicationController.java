package io.rxbridge.enrollment.medication;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

import static io.rxbridge.enrollment.medication.MedicationConstants.*;

@RestController
@RequestMapping("/medications")
@RequiredArgsConstructor
class MedicationController {

    record RegisterRequest(
        @NotBlank @Size(max = NAME_MAX_LENGTH) String name,
        @Size(max = DESCRIPTION_MAX_LENGTH) String description,
        @NotBlank @MedicationCode String code,
        @Size(max = SIDE_EFFECTS_MAX_LENGTH) String sideEffects
    ) { }

    record RenameRequest(
        @NotBlank @Size(max = NAME_MAX_LENGTH) String name
    ) { }

    record DescriptionRequest(
        @Size(max = DESCRIPTION_MAX_LENGTH) String description
    ) { }

    record SideEffectsRequest(
        @Size(max = SIDE_EFFECTS_MAX_LENGTH) String sideEffects
    ) { }

    private final MedicationService medications;
    private final MedicationQueries queries;

    @PostMapping
    ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        UUID id = medications.register(
            request.name(),
            request.description(),
            request.code(),
            request.sideEffects()
        );

        return ResponseEntity.created(
            URI.create("/medications/" + id)
        ).build();
    }

    @GetMapping("/{id}")
    MedicationView medication(@PathVariable UUID id) {
        return queries.medication(id);
    }

    @GetMapping("/by-code/{code}")
    MedicationView medication(@PathVariable @MedicationCode String code) {
        return queries.medication(code);
    }

    @PatchMapping("/{id}/name")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void rename(
        @PathVariable UUID id,
        @Valid @RequestBody RenameRequest request
    ) {
        medications.rename(id, request.name());
    }

    @PostMapping("/{id}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deactivate(@PathVariable UUID id) {
        medications.deactivate(id);
    }

    @PostMapping("/{id}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void activate(@PathVariable UUID id) {
        medications.activate(id);
    }

    @PatchMapping("/{id}/description")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changeDescription(
        @PathVariable UUID id,
        @Valid @RequestBody DescriptionRequest request
    ) {
        medications.changeDescription(id, request.description());
    }

    @PatchMapping("/{id}/side-effects")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changeSideEffects(
        @PathVariable UUID id,
        @Valid @RequestBody SideEffectsRequest request
    ) {
        medications.changeSideEffects(id, request.sideEffects());
    }

}
