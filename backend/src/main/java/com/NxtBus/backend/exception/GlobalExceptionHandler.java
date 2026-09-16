package com.nxtbus.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Handles every NxtBus domain exception in one place.
    @ExceptionHandler(NxtBusException.class)
    public ProblemDetail handleNxtBusException(NxtBusException ex) {
        ErrorCode code = ex.getErrorCode();

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(code.getStatus(), ex.getMessage());
        pd.setTitle(code.getTitle());
        pd.setProperty("errorCode", code.name());
        pd.setProperty("timestamp", Instant.now());
        ex.getProperties().forEach(pd::setProperty);

        return pd;
    }

    // Bean validation failures on @Valid request bodies — not a domain
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ErrorCode.VALIDATION_FAILED.getStatus(), message);
        pd.setTitle(ErrorCode.VALIDATION_FAILED.getTitle());
        pd.setProperty("errorCode", ErrorCode.VALIDATION_FAILED.name());
        pd.setProperty("timestamp", Instant.now());

        return pd;
    }

    // Catch-all fallback for anything unexpected/unhandled.
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong");
        pd.setTitle(ErrorCode.INTERNAL_ERROR.getTitle());
        pd.setProperty("errorCode", ErrorCode.INTERNAL_ERROR.name());
        pd.setProperty("timestamp", Instant.now());

        return pd;
    }
}