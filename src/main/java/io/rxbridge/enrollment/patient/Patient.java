package io.rxbridge.enrollment.patient;

import io.rxbridge.enrollment.common.persistence.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.EmbeddedColumnNaming;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

import static io.rxbridge.enrollment.common.validation.DomainPreconditions.*;
import static io.rxbridge.enrollment.patient.PatientConstants.*;

@Entity
@Table(name = "patient")
public class Patient extends BaseEntity {

    @Valid
    @NotNull
    @Embedded
    @AttributeOverride(
        name = "firstName", column = @Column(
            name = "first_name",
            nullable = false,
            length = NAME_MAX_LENGTH
        )
    )
    @AttributeOverride(
        name = "lastName", column = @Column(
            name = "last_name",
            nullable = false,
            length = NAME_MAX_LENGTH
        )
    )
    private PersonName personName;

    @NotBlank
    @Size(max = EMAIL_MAX_LENGTH)
    @Pattern(regexp = EMAIL_PATTERN)
    @Column(name = "email", nullable = false, length = EMAIL_MAX_LENGTH)
    private String email;

    @Valid
    @NotNull
    @Embedded
    @EmbeddedColumnNaming("address_%s")
    private Address address;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "sex", nullable = false, length = SEX_MAX_LENGTH)
    private Sex sex;

    @NotNull
    @Column(name = "birthdate", nullable = false)
    private LocalDate birthdate;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected Patient() {}

    public Patient(
        PersonName personName,
        String email,
        Address address,
        Sex sex,
        LocalDate birthdate,
        LocalDate today
    ) {
        this.personName = Objects.requireNonNull(personName, "personName");
        this.email = validateEmail(email);
        this.address = Objects.requireNonNull(address, "address");
        this.sex = Objects.requireNonNull(sex, "sex");
        this.birthdate = requireNotFuture(birthdate, today, "birthdate");
    }

    public PersonName getPersonName() {
        return personName;
    }

    public String getEmail() {
        return email;
    }

    public Address getAddress() {
        return address;
    }

    public Sex getSex() {
        return sex;
    }

    public LocalDate getBirthdate() {
        return birthdate;
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public void changeEmail(String email) {
        this.email = validateEmail(email);
    }

    public void changeAddress(Address address) {
        this.address = Objects.requireNonNull(address, "address");
    }

    private String validateEmail(String email) {
        email = requireText(email, "email", EMAIL_MAX_LENGTH);
        email = email.toLowerCase(Locale.ROOT);
        email = requireMatch(email, "email", EMAIL_PATTERN);

        return email;
    }

}
