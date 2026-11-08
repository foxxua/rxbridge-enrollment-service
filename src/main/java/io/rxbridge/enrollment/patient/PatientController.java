package io.rxbridge.enrollment.patient;

import io.rxbridge.enrollment.common.error.DomainValidationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
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
class PatientController {

    private static final int MAX_DUPLICATES = 50;
    private static final String CANNOT_SORT_BY_TEMPLATE = "Cannot sort by %s";
    private static final Set<String> SORTABLE = Set.of(
        "personName.firstName",
        "personName.lastName"
    );

    record RegisterRequest(
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
    ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
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
    PatientView patient(@PathVariable UUID id) {
        return queries.patient(id);
    }

    @PostMapping("/duplicates")
    List<PatientView> duplicatesOf(
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
    PagedModel<PatientView> findPatients(
        @Valid @RequestBody EmailRequest request,
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
    void deactivate(@PathVariable UUID id) {
        patients.deactivate(id);
    }

    @PostMapping("/{id}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void activate(@PathVariable UUID id) {
        patients.activate(id);
    }

    @PatchMapping("/{id}/email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changeEmail(
        @PathVariable UUID id,
        @Valid @RequestBody EmailRequest request
    ) {
        patients.changeEmail(id, request.email());
    }

    @PatchMapping("/{id}/address")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changeAddress(
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
