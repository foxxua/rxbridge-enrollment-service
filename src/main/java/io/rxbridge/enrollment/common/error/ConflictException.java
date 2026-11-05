package io.rxbridge.enrollment.common.error;

public abstract class ConflictException extends RuntimeException {

    protected ConflictException(String message) {
        super(message);
    }

    protected ConflictException(String message, Throwable cause) {
        super(message, cause);
    }

}
