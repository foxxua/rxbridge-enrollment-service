package io.rxbridge.enrollment.enrollment;

import io.rxbridge.enrollment.common.error.ConflictException;

import java.util.UUID;

public class CloseReasonMismatchException extends ConflictException {

    private static final String TEMPLATE = "close reason %s but %s %s is active";

    private final CloseReason reason;
    private final String targetName;
    private final UUID targetId;

    public CloseReasonMismatchException(
        CloseReason reason,
        String targetName,
        UUID targetId
    ) {
        super(TEMPLATE.formatted(reason, targetName, targetId));
        this.reason = reason;
        this.targetName = targetName;
        this.targetId = targetId;
    }

    public CloseReason reason() {
        return this.reason;
    }

    public String targetName() {
        return this.targetName;
    }

    public UUID targetId() {
        return this.targetId;
    }

}
