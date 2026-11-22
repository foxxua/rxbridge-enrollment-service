package io.rxbridge.enrollment.patient;

import io.rxbridge.enrollment.common.error.DomainValidationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static io.rxbridge.enrollment.patient.PatientConstants.*;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
@Tag(name = "Patients", description = "Operations with medications: register, find, change")
class PatientController {

    private static final int MAX_DUPLICATES = 50;
    private static final String CANNOT_SORT_BY_TEMPLATE = "Cannot sort by %s";
    private static final Set<String> SORTABLE = Set.of(
        "personName.firstName",
        "personName.lastName",
        "address.street",
        "address.city",
        "address.country"
    );

    record PatientRegisterRequest(
        @Valid @NotNull PersonNameRequest personName,
        @NotBlank @Email @Size(max = EMAIL_MAX_LENGTH) String email,
        @Valid @NotNull AddressRequest address,
        @NotNull Sex sex,
        @NotNull LocalDate birthdate
    ) { }

    record PersonNameRequest(
        @NotBlank @Size(max = NAME_MAX_LENGTH) String firstName,
        @NotBlank @Size(max = NAME_MAX_LENGTH) String lastName
    ) {
        PersonName toPersonName() {
            return new PersonName(firstName, lastName);
        }
    }

    record AddressRequest(
        @NotBlank @Size(max = STREET_MAX_LENGTH) String street,
        @NotBlank @Size(max = CITY_MAX_LENGTH) String city,
        @NotBlank @Size(max = COUNTRY_MAX_LENGTH) String country,
        @Size(max = REGION_MAX_LENGTH) String region,
        @NotBlank @Size(max = ZIPCODE_MAX_LENGTH) String zipcode
    ) {
        Address toAddress() {
            return new Address(
                street,
                city,
                country,
                region,
                zipcode
            );
        }
    }

    record DuplicatesRequest(
        @Valid @NotNull PersonNameRequest personName,
        @NotNull LocalDate birthdate
    ) { }

    record EmailRequest(
        @NotBlank @Email @Size(max = EMAIL_MAX_LENGTH) String email
    ) { }

    private final PatientService patients;
    private final PatientQueries queries;

    @PostMapping
    @Operation(summary = "Register (create) a patient in the system")
    @ApiResponse(
        responseCode = "201", description = "Patient registered (created)",
        headers = @Header(
            name = "Location", description = "Address of the new patient",
            schema = @Schema(type = "string")
        )
    )
    @ApiResponse(
        responseCode = "409",
        description = "Duplicate, patient with the same name and birthdate is already created"
    )
    ResponseEntity<Void> registerPatient(@Valid @RequestBody PatientRegisterRequest request) {
        UUID id = patients.register(
            request.personName().toPersonName(),
            request.email(),
            request.address().toAddress(),
            request.sex(),
            request.birthdate()
        );

        return ResponseEntity.created(
            URI.create("/patients/" + id)
        ).build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Read a patient by Id")
    @ApiResponse(responseCode = "200", description = "The patient")
    @ApiResponse(responseCode = "404", description = "Patient not found")
    PatientView findPatientById(@PathVariable UUID id) {
        return queries.patient(id);
    }

    @PostMapping("/duplicates")
    @Operation(summary = "Find duplicate patient - same name or birthdate")
    List<PatientView> findPatientsDuplicates(
        @Valid @RequestBody DuplicatesRequest request,
        @RequestParam(defaultValue = "10") @Min(1) @Max(MAX_DUPLICATES)
        int limit
    ) {
        return queries.duplicatesOf(
            request.personName().toPersonName(),
            request.birthdate(),
            Limit.of(limit)
        );
    }

    @PostMapping("/find")
    @Operation(
        summary = "Find patients by email",
        description = "Sortable by personName.firstName, personName.lastName, address.street, address.city and address.country. A size above 100 is cut down to 100"
    )
    PagedModel<PatientView> findPatients(
        @Valid @RequestBody EmailRequest request,
        @ParameterObject
        @PageableDefault(size = 20, sort = {"personName.lastName", "personName.firstName"}, direction = Sort.Direction.ASC)
        Pageable pageable
    ) {
        requireSortable(pageable.getSort());
        Pageable ordered = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            pageable.getSort().and(Sort.by("id"))
        );

        return new PagedModel<>(queries.patientsOf(
            request.email(),
            ordered
        ));
    }

    @PostMapping("/{id}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivating a patient")
    @ApiResponse(responseCode = "204", description = "Patient is deactivated")
    @ApiResponse(responseCode = "404", description = "Patient is not found")
    @ApiResponse(
        responseCode = "409",
        description = "Patient is deactivating by more then one request (two or more threads) at the same time"
    )
    void deactivatePatient(@PathVariable UUID id) {
        patients.deactivate(id);
    }

    @PostMapping("/{id}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Activating a patient")
    @ApiResponse(responseCode = "204", description = "Patient is activated")
    @ApiResponse(responseCode = "404", description = "Patient is not found")
    @ApiResponse(
        responseCode = "409",
        description = "Patient is activating by more then one request (two or more threads) at the same time"
    )
    void activatePatient(@PathVariable UUID id) {
        patients.activate(id);
    }

    @PatchMapping("/{id}/email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Changing a patient email")
    @ApiResponse(responseCode = "204", description = "Patient email is changed")
    @ApiResponse(responseCode = "404", description = "Patient is not found")
    @ApiResponse(
        responseCode = "409",
        description = "Patient email is changing by more then one request (two or more threads) at the same time"
    )
    void changePatientEmail(
        @PathVariable UUID id,
        @Valid @RequestBody EmailRequest request
    ) {
        patients.changeEmail(id, request.email());
    }

    @PatchMapping("/{id}/address")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Changing a patient address")
    @ApiResponse(responseCode = "204", description = "Patient address is changed")
    @ApiResponse(responseCode = "404", description = "Patient is not found")
    @ApiResponse(
        responseCode = "409",
        description = "Patient address is changing by more then one request (two or more threads) at the same time"
    )
    void changePatientAddress(
        @PathVariable UUID id,
        @Valid @RequestBody AddressRequest request
    ) {
        patients.changeAddress(id, request.toAddress());
    }

    private static void requireSortable(Sort sort) {
        for (Sort.Order order : sort) {
            if (!SORTABLE.contains(order.getProperty())) {
                throw new DomainValidationException(
                    CANNOT_SORT_BY_TEMPLATE.formatted(order.getProperty())
                );
            }
        }
    }

}
