package io.rxbridge.enrollment.common.persistence;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

public final class DatabaseConstraints {

    private DatabaseConstraints() { }

    public static boolean violates(DataIntegrityViolationException e, String constraintName) {
        return e.getCause() instanceof ConstraintViolationException violation
            && constraintName.equals(violation.getConstraintName());
    }

}
