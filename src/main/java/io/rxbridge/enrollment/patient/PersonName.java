package io.rxbridge.enrollment.patient;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static io.rxbridge.enrollment.common.validation.DomainPreconditions.*;
import static io.rxbridge.enrollment.patient.PatientConstants.*;

@Embeddable
public record PersonName(

    @NotBlank
    @Size(max = NAME_MAX_LENGTH)
    String firstName,

    @NotBlank
    @Size(max = NAME_MAX_LENGTH)
    String lastName
) {

    public PersonName {
        firstName = requireText(firstName, "firstName", NAME_MAX_LENGTH);
        lastName = requireText(lastName, "lastName", NAME_MAX_LENGTH);
    }

    public String fullName() {
        return this.firstName + " " + this.lastName;
    }

}
