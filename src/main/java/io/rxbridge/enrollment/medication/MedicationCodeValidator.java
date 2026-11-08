package io.rxbridge.enrollment.medication;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

import static io.rxbridge.enrollment.medication.MedicationConstants.CODE_PATTERN;

public class MedicationCodeValidator implements ConstraintValidator<MedicationCode, String> {

    private static final Pattern CODE = Pattern.compile(CODE_PATTERN);

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.isBlank() || validate(value);
    }

    private boolean validate(String code) {
        code = Medication.normalizeCode(code);
        return CODE.matcher(code).matches();
    }

}
