package io.rxbridge.enrollment.medication;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Medications", description = "Operations with medications: register, find, change")
class MedicationController {

    record MedicationRegisterRequest(
        @NotBlank @Size(max = NAME_MAX_LENGTH) String name,
        @Size(max = DESCRIPTION_MAX_LENGTH) String description,

        @NotBlank @MedicationCode
        @Schema(
            description = "Medication code - unique value. Format - MXXXXXX, where X - is a digit",
            example = "M123456")
        String code,

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
    @Operation(summary = "Register (create) a medication in the system")
    @ApiResponse(
        responseCode = "201", description = "Medication registered (created)",
        headers = @Header(
            name = "Location", description = "Address of the new medication",
            schema = @Schema(type = "string")
        )
    )
    @ApiResponse(
        responseCode = "409", description = "Duplicate, medication with the code is already created"
    )
    ResponseEntity<Void> registerMedication(@Valid @RequestBody MedicationRegisterRequest request) {
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
    @Operation(summary = "Read a medication by Id")
    @ApiResponse(responseCode = "200", description = "The medication")
    @ApiResponse(responseCode = "404", description = "Medication not found")
    MedicationView findMedicationById(@PathVariable UUID id) {
        return queries.medication(id);
    }

    @GetMapping("/by-code/{code}")
    @Operation(summary = "Read a medication by medication code")
    @ApiResponse(responseCode = "200", description = "The medication")
    @ApiResponse(responseCode = "404", description = "Medication not found")
    MedicationView findMedicationByCode(@PathVariable @MedicationCode String code) {
        return queries.medication(code);
    }

    @PatchMapping("/{id}/name")
    @Operation(summary = "Rename medication")
    @ApiResponse(responseCode = "204", description = "Requested medication was renamed")
    @ApiResponse(responseCode = "404", description = "Medication is not found")
    @ApiResponse(
        responseCode = "409",
        description = "Medication is renaming by more then one request (two or more threads) at the same time"
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void renameMedication(
        @PathVariable UUID id,
        @Valid @RequestBody RenameRequest request
    ) {
        medications.rename(id, request.name());
    }

    @PostMapping("/{id}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivating a medication")
    @ApiResponse(responseCode = "204", description = "Medication is deactivated")
    @ApiResponse(responseCode = "404", description = "Medication is not found")
    @ApiResponse(
        responseCode = "409",
        description = "Medication is deactivating by more then one request (two or more threads) at the same time"
    )
    void deactivateMedication(@PathVariable UUID id) {
        medications.deactivate(id);
    }

    @PostMapping("/{id}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Activating a medication")
    @ApiResponse(responseCode = "204", description = "Medication is activated")
    @ApiResponse(responseCode = "404", description = "Medication is not found")
    @ApiResponse(
        responseCode = "409",
        description = "Medication is activating by more then one request (two or more threads) at the same time"
    )
    void activateMedication(@PathVariable UUID id) {
        medications.activate(id);
    }

    @PatchMapping("/{id}/description")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Changing a medication description")
    @ApiResponse(responseCode = "204", description = "Medication description is changed")
    @ApiResponse(responseCode = "404", description = "Medication is not found")
    @ApiResponse(
        responseCode = "409",
        description = "Medication description is changing by more then one request (two or more threads) at the same time"
    )
    void changeMedicationDescription(
        @PathVariable UUID id,
        @Valid @RequestBody DescriptionRequest request
    ) {
        medications.changeDescription(id, request.description());
    }

    @PatchMapping("/{id}/side-effects")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Changing a medication side effects")
    @ApiResponse(responseCode = "204", description = "Medication side effects is changed")
    @ApiResponse(responseCode = "404", description = "Medication is not found")
    @ApiResponse(
        responseCode = "409",
        description = "Medication side effects is changing by more then one request (two or more threads) at the same time"
    )
    void changeMedicationSideEffects(
        @PathVariable UUID id,
        @Valid @RequestBody SideEffectsRequest request
    ) {
        medications.changeSideEffects(id, request.sideEffects());
    }

}
