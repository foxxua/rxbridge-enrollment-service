package io.rxbridge.enrollment.medication;

import io.rxbridge.enrollment.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Locale;

import static io.rxbridge.enrollment.common.validation.DomainPreconditions.*;
import static io.rxbridge.enrollment.medication.MedicationConstants.*;

@Entity
@Table(name = "medication")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Medication extends BaseEntity {

    @NotBlank
    @Size(max = NAME_MAX_LENGTH)
    @Column(name = "name", nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @Size(max = DESCRIPTION_MAX_LENGTH)
    @Column(name = "description", length = DESCRIPTION_MAX_LENGTH)
    private String description;

    @NotBlank
    @Size(max = CODE_LENGTH, min = CODE_LENGTH)
    @Pattern(regexp = CODE_PATTERN)
    @Column(name = "code", nullable = false, updatable = false, length = CODE_LENGTH)
    private String code;

    @Size(max = SIDE_EFFECTS_MAX_LENGTH)
    @Column(name = "side_effects", length = SIDE_EFFECTS_MAX_LENGTH)
    private String sideEffects;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public Medication(
        String name,
        String description,
        String code,
        String sideEffects
    ) {
        this.name = requireText(name, "name", NAME_MAX_LENGTH);
        this.description = optionalText(description, "description", DESCRIPTION_MAX_LENGTH);
        this.code = validateCode(code);
        this.sideEffects = optionalText(sideEffects, "sideEffects", SIDE_EFFECTS_MAX_LENGTH);
    }

    public void rename(String name) {
        this.name = requireText(name, "name", NAME_MAX_LENGTH);
    }

    public void changeDescription(String description) {
        this.description = optionalText(description, "description", DESCRIPTION_MAX_LENGTH);
    }

    public void changeSideEffects(String sideEffects) {
        this.sideEffects = optionalText(sideEffects, "sideEffects", SIDE_EFFECTS_MAX_LENGTH);
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    private String validateCode(String code) {
        code = requireText(code, "code", CODE_LENGTH);
        code = normalizeCode(code);
        return requireMatch(code, "code", CODE_PATTERN);
    }

    public static String normalizeCode(String code) {
        return code.strip().toUpperCase(Locale.ROOT);
    }

}
