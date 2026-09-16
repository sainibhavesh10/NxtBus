package com.nxtbus.backend.exception;

import java.util.Collections;
import java.util.Map;

/**
 * Base class for all domain exceptions in NxtBus.
 * */
public abstract class NxtBusException extends RuntimeException {

    private final ErrorCode errorCode;

    protected NxtBusException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    protected NxtBusException(String message, ErrorCode errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getProperties() {
        return Collections.emptyMap();
    }
}