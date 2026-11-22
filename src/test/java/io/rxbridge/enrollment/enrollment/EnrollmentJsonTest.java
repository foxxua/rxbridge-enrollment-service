package io.rxbridge.enrollment.enrollment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import tools.jackson.databind.exc.InvalidFormatException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

import static io.rxbridge.enrollment.enrollment.EnrollmentController.CloseRequest;
import static io.rxbridge.enrollment.enrollment.EnrollmentController.CompleteRequest;
import static io.rxbridge.enrollment.enrollment.EnrollmentController.ApproveRequest;
import static io.rxbridge.enrollment.enrollment.EnrollmentController.EnrollRequest;
import static io.rxbridge.enrollment.common.web.TestHelpers.*;
import static io.rxbridge.enrollment.enrollment.EnrollmentFixtures.*;

@JsonTest
class EnrollmentJsonTest {

    @Autowired
    JacksonTester<EnrollmentView> view;

    @Autowired
    JacksonTester<EnrollRequest> enroll;

    @Autowired
    JacksonTester<ApproveRequest> approve;

    @Autowired
    JacksonTester<CompleteRequest> complete;

    @Autowired
    JacksonTester<CloseRequest> close;

    @Test
    void viewKeepsAgreedContract() throws Exception {
        Enrollment enrollment = withId(aCancelledEnrollmentFromApproved(), ENROLLMENT_ID);
        withId(enrollment.getMedication(), MEDICATION_ID);

        var enrollmentView = EnrollmentView.from(enrollment);

        assertThat(view.write(enrollmentView))
            .isStrictlyEqualToJson("""
                {
                  "id":"00000000-0000-0000-0000-000000000003",
                  "medication":{
                    "id":"00000000-0000-0000-0000-000000000002",
                    "name":"Aspirin",
                    "code":"M000001",
                    "active":true
                  },
                  "status":"CANCELLED",
                  "startDate":"2025-08-10",
                  "closure":{
                    "closeReason":"OTHER",
                    "otherReason":"Other Reason Text",
                    "closeDate":"2026-09-01"
                  }
                }
                """);
    }

    @Test
    void enrollReadsValidUuidFormat() throws Exception {
        String enrollJson = """
            {"medicationId":"00000000-0000-0000-0000-000000000002"}
            """;

        assertThat(enroll.parseObject(enrollJson).medicationId())
            .isEqualTo(MEDICATION_ID);
    }

    @Test
    void enrollRefusesInvalidUuid() throws Exception {
        String enrollJson = """
            {"medicationId":"abc"}
            """;

        assertThatThrownBy(() -> enroll.parseObject(enrollJson))
            .isInstanceOf(InvalidFormatException.class);
    }

    @Test
    void approveReadsAnIsoDate() throws Exception {
        String approveJson = """
            {"startDate":"2025-08-10"}
            """;

        assertThat(approve.parseObject(approveJson).startDate())
            .isEqualTo(LocalDate.of(2025, 8, 10));
    }

    @Test
    void approveRefusesAnotherDateFormat() throws Exception {
        String approveJson = """
            {"startDate":"10.08.2025"}
            """;

        assertThatThrownBy(() -> approve.parse(approveJson))
            .isInstanceOf(InvalidFormatException.class);
    }

    @Test
    void completeReadsAnIsoDate() throws Exception {
        String completeJson = """
            {"closeDate":"2026-09-01"}
            """;

        assertThat(complete.parseObject(completeJson).closeDate())
            .isEqualTo(LocalDate.of(2026, 9, 1));
    }

    @Test
    void completeRefusesAnotherDateFormat() throws Exception {
        String completeJson = """
            {"closeDate":"1.9.2026"}
            """;

        assertThatThrownBy(() -> complete.parse(completeJson))
            .isInstanceOf(InvalidFormatException.class);
    }

    @ParameterizedTest
    @EnumSource(CloseReason.class)
    void closeReadsValidValues(CloseReason closeReason) throws Exception {
        String closeJson = """
            {
              "closeDate":"2026-09-01",
              "closeReason":"%s",
              "otherReason":"Other Reason Text"
            }
            """.formatted(closeReason);

        assertThat(close.parseObject(closeJson))
            .isEqualTo(new CloseRequest(
                LocalDate.of(2026, 9, 1),
                closeReason,
                "Other Reason Text"
            ));
    }

    @Test
    void closeRefusesAnotherCloseReason() throws Exception {
        String closeJson = """
            {
              "closeDate":"2026-09-01",
              "closeReason":"ANOTHER",
              "otherReason":"Other Reason Text"
            }
            """;

        assertThatThrownBy(() -> close.parse(closeJson))
            .isInstanceOf(InvalidFormatException.class);
    }

    @Test
    void closeRefusesAnotherDateFormat() throws Exception {
        String closeJson = """
            {
              "closeDate":"01.09.2026",
              "closeReason":"OTHER",
              "otherReason":"Other Reason Text"
            }
            """;

        assertThatThrownBy(() -> close.parse(closeJson))
            .isInstanceOf(InvalidFormatException.class);
    }

}
