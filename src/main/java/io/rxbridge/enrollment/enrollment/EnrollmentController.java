package io.rxbridge.enrollment.enrollment;

import io.rxbridge.enrollment.common.error.DomainValidationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
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
import java.util.Set;
import java.util.UUID;

import static io.rxbridge.enrollment.enrollment.EnrollmentConstants.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Enrollments", description = "Enrolls the patients for medications")
class EnrollmentController {

    private static final String CANNOT_SORT_BY_TEMPLATE = "Cannot sort by %s";
    private static final Set<String> SORTABLE = Set.of("startDate", "closure.closeDate", "status");

    record EnrollRequest(
        @NotNull
        @Schema(description = "The medication to enroll")
        UUID medicationId
    ) {}

    record ApproveRequest(
        @NotNull
        @Schema(description = "The first day of the enrollment", example = "2026-10-14")
        LocalDate startDate
    ) {}

    record CompleteRequest(
        @NotNull
        @Schema(description = "The last day of the enrollment", example = "2026-10-14")
        LocalDate closeDate
    ) {}

    record CloseRequest(
        @NotNull
        @Schema(description = "The last day of the enrollment", example = "2026-10-14")
        LocalDate closeDate,

        @NotNull CloseReason closeReason,

        @Schema(description = "Free text for the reason OTHER")
        @Size(max = OTHER_REASON_MAX_LENGTH) String otherReason
    ) {
        Closure toClosure() {
            return new Closure(closeReason, otherReason);
        }
    }

    private final EnrollmentService enrollments;
    private final EnrollmentQueries queries;

    @PostMapping("/patients/{patientId}/enrollments")
    @Operation(summary = "Enroll the medication for the patient")
    @ApiResponse(
        responseCode = "201", description = "Enrollment created",
        headers = @Header(
            name = "Location", description = "Address of the new enrollment",
            schema = @Schema(type = "string")
        )
    )
    @ApiResponse(
        responseCode = "404", description = "Patient or medication not found"
    )
    @ApiResponse(
        responseCode = "409",
        description = "Duplicate, patient or medication not active"
    )
    ResponseEntity<Void> enrollPatient(
        @PathVariable UUID patientId,
        @Valid @RequestBody EnrollRequest request
    ) {
        UUID id = enrollments.enroll(patientId, request.medicationId());
        return ResponseEntity.created(
            URI.create("/enrollments/" + id)
        ).build();
    }

    @GetMapping("/patients/{patientId}/enrollments")
    @Operation(
        summary = "List of the enrollments of a patient",
        description = "Sortable by startDate, closure.closeDate and status. A size above 100 is cut down to 100"
    )
    PagedModel<EnrollmentView> findPatientEnrollments(
        @PathVariable UUID patientId,
        @ParameterObject
        @PageableDefault(size = 20, sort = "startDate", direction = Sort.Direction.DESC)
        Pageable pageable
    ) {
        requireSortable(pageable.getSort());
        Pageable ordered = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            pageable.getSort().and(Sort.by("id"))
        );
        return new PagedModel<>(queries.enrollmentsOf(patientId, ordered));
    }

    @GetMapping("/enrollments/{id}")
    @Operation(summary = "Read one enrollment")
    @ApiResponse(responseCode = "200", description = "The enrollment")
    @ApiResponse(responseCode = "404", description = "Enrollment not found")
    EnrollmentView findEnrollmentById(@PathVariable UUID id) {
        return queries.enrollment(id);
    }

    @PostMapping("/enrollments/{id}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Approve a pending enrollment")
    @ApiResponse(responseCode = "204", description = "Approved")
    @ApiResponse(responseCode = "404", description = "Enrollment not found")
    @ApiResponse(responseCode = "409", description = "Not in the PENDING status, or the patient is not active")
    void approveEnrollment(
        @PathVariable UUID id,
        @Valid @RequestBody ApproveRequest request
    ) {
        enrollments.approve(id, request.startDate());
    }

    @PostMapping("/enrollments/{id}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Reject a pending enrollment")
    @ApiResponse(responseCode = "204", description = "Rejected")
    @ApiResponse(responseCode = "404", description = "Enrollment not found")
    @ApiResponse(responseCode = "409", description = "Not in the PENDING status")
    void rejectEnrollment(
        @PathVariable UUID id,
        @Valid @RequestBody CloseRequest request
    ) {
        enrollments.reject(id, request.toClosure(), request.closeDate());
    }

    @PostMapping("/enrollments/{id}/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Complete an approved enrollment")
    @ApiResponse(responseCode = "204", description = "Completed")
    @ApiResponse(responseCode = "404", description = "Enrollment not found")
    @ApiResponse(responseCode = "409", description = "Not in the APPROVED status")
    void completeEnrollment(
        @PathVariable UUID id,
        @Valid @RequestBody CompleteRequest request
    ) {
        enrollments.complete(id, request.closeDate());
    }

    @PostMapping("/enrollments/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancel an enrollment of the current patient")
    @ApiResponse(responseCode = "204", description = "Cancelled")
    @ApiResponse(responseCode = "404", description = "Enrollment not found, or it belongs to another patient")
    @ApiResponse(responseCode = "409", description = "Already closed, or the reason is not true")
    void cancelEnrollment(
        @PathVariable UUID id,
        @Parameter(description = "A stand-in until Spring Security")
        @RequestHeader("X-Patient-Id") UUID patientId,
        @Valid @RequestBody CloseRequest request
    ) {
        enrollments.cancelByPatient(
            id,
            patientId,
            request.toClosure(),
            request.closeDate()
        );
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
