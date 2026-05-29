package com.ipscentir.appointments.presentation.rest.exception;

import com.ipscentir.appointments.application.dto.admin.FacilityHoursViolationErrorResponse;
import com.ipscentir.appointments.application.exception.SedeNotFoundException;
import com.ipscentir.appointments.application.exception.MedicoNotFoundException;
import com.ipscentir.appointments.application.exception.FacilityOperatingHoursViolationException;
import com.ipscentir.appointments.application.exception.ResourceCapacityExceededException;
import com.ipscentir.appointments.application.exception.PatientAlreadyExistsException;
import com.ipscentir.appointments.application.exception.PatientNotFoundException;
import com.ipscentir.appointments.infrastructure.observability.AppointmentsMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final AppointmentsMetrics appointmentsMetrics;

    @ExceptionHandler(SedeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSedeNotFoundException(SedeNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MedicoNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMedicoNotFoundException(MedicoNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({PatientNotFoundException.class, PatientAlreadyExistsException.class})
    public ResponseEntity<ErrorResponse> handlePatientExceptions(RuntimeException ex) {
        HttpStatus status = ex instanceof PatientAlreadyExistsException
                ? HttpStatus.CONFLICT
                : HttpStatus.NOT_FOUND;
        ErrorResponse error = new ErrorResponse(status.value(), ex.getMessage(), LocalDateTime.now());
        return new ResponseEntity<>(error, status);
    }

    @ExceptionHandler(FacilityOperatingHoursViolationException.class)
    public ResponseEntity<FacilityHoursViolationErrorResponse> handleFacilityOperatingHoursViolation(
            FacilityOperatingHoursViolationException ex
    ) {
        FacilityHoursViolationErrorResponse error = new FacilityHoursViolationErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                ex.getMessage(),
                LocalDateTime.now(),
                ex.sedeId(),
                ex.sedeNombre(),
                ex.allowedWindow()
        );
        return new ResponseEntity<>(error, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceCapacityExceededException.class)
    public ResponseEntity<ErrorResponse> handleResourceCapacityExceeded(ResourceCapacityExceededException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            errors.put(violation.getPropertyPath().toString(), violation.getMessage());
        }
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        appointmentsMetrics.recordForbidden();
        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unhandled exception", ex);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public record ErrorResponse(int status, String message, LocalDateTime timestamp) {}
}
