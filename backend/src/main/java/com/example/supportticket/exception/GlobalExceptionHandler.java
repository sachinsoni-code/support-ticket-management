package com.example.supportticket.exception;

import com.example.supportticket.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setCode("VALIDATION_ERROR");
        if (!ex.getBindingResult().getAllErrors().isEmpty()) {
            ObjectError firstError = ex.getBindingResult().getAllErrors().get(0);
            String message;
            if (firstError instanceof FieldError) {
                FieldError fieldError = (FieldError) firstError;
                message = fieldError.getField() + ": " + fieldError.getDefaultMessage();
            } else {
                message = firstError.getDefaultMessage();
            }
            errorResponse.setMessage(message);
        } else {
            errorResponse.setMessage("Validation failed for one or more arguments.");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setCode("MALFORMED_REQUEST");
        errorResponse.setMessage("Malformed JSON request.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setCode("MALFORMED_REQUEST");
        errorResponse.setMessage("Request parameter has invalid type.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTicketNotFound(TicketNotFoundException ex) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setCode("TICKET_NOT_FOUND");
        errorResponse.setMessage(ex.getMessage() != null ? ex.getMessage() : "Ticket not found.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStatusTransition(InvalidStatusTransitionException ex) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setCode("INVALID_STATUS_TRANSITION");
        errorResponse.setMessage(ex.getMessage() != null ? ex.getMessage() : "Invalid status transition.");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        // Detect invalid enum, path, or query value errors for MALFORMED_REQUEST
        String message = ex.getMessage() != null ? ex.getMessage() : "Invalid argument.";
        boolean isMalformed = false;

        // Heuristic: enum parsing/path/query errors usually have this form:
        // "No enum constant com.example.supportticket.entity.Ticket.Status.INVALID"
        if (message != null && message.startsWith("No enum constant")) {
            isMalformed = true;
        } else {
            // Also check for Enum.valueOf calls in the stack trace
            StackTraceElement[] stackTrace = ex.getStackTrace();
            if (stackTrace.length > 0 && stackTrace[0].getMethodName().contains("valueOf")) {
                isMalformed = true;
            }
        }

        ErrorResponse errorResponse = new ErrorResponse();
        if (isMalformed) {
            errorResponse.setCode("MALFORMED_REQUEST");
        } else {
            errorResponse.setCode("VALIDATION_ERROR");
        }
        errorResponse.setMessage(message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllOtherExceptions(Exception ex) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setCode("INTERNAL_ERROR");
        errorResponse.setMessage("An unexpected error occurred.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}