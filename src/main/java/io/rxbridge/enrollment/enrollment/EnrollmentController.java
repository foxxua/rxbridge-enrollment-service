package io.rxbridge.enrollment.enrollment;

import io.rxbridge.enrollment.common.error.DomainValidationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
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
class EnrollmentController {

    private static final String CANNOT_SORT_BY_TEMPLATE = "Cannot sort by %s";
    private static final Set<String> SORTABLE = Set.of("startDate", "closure.closeDate", "status");

    record EnrollRequest(@NotNull UUID medicationId) {}
    record ApproveRequest(@NotNull LocalDate startDate) {}
    record CompleteRequest(@NotNull LocalDate closeDate) {}

    record CloseRequest(
        @NotNull LocalDate closeDate,
        @NotNull CloseReason closeReason,
        @Size(max = OTHER_REASON_MAX_LENGTH) String otherReason
    ) {
        Closure toClosure() {
            return new Closure(closeReason, otherReason);
        }
    }

    private final EnrollmentService enrollments;
    private final EnrollmentQueries queries;

    @PostMapping("/patients/{patientId}/enrollments")
    ResponseEntity<Void> enroll(
        @PathVariable UUID patientId,
        @Valid @RequestBody EnrollRequest request
    ) {
        UUID id = enrollments.enroll(patientId, request.medicationId());
        return ResponseEntity.created(
            URI.create("/enrollments/" + id)
        ).build();
    }

    @GetMapping("/patients/{patientId}/enrollments")
    PagedModel<EnrollmentView> enrollmentsOf(
        @PathVariable UUID patientId,
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
    EnrollmentView enrollment(@PathVariable UUID id) {
        return queries.enrollment(id);
    }

    @PostMapping("/enrollments/{id}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void approve(
        @PathVariable UUID id,
        @Valid @RequestBody ApproveRequest request
    ) {
        enrollments.approve(id, request.startDate());
    }

    @PostMapping("/enrollments/{id}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void reject(
        @PathVariable UUID id,
        @Valid @RequestBody CloseRequest request
    ) {
        enrollments.reject(id, request.toClosure(), request.closeDate());
    }

    @PostMapping("/enrollments/{id}/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void complete(
        @PathVariable UUID id,
        @Valid @RequestBody CompleteRequest request
    ) {
        enrollments.complete(id, request.closeDate());
    }

    @PostMapping("/enrollments/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancel(
        @PathVariable UUID id,
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
