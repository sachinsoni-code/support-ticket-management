package com.example.supportticket.exception;

/**
 * Exception thrown when an invalid status transition is attempted
 * in the Support Ticket Management System.
 */
public class InvalidStatusTransitionException extends RuntimeException {
    
    /**
     * Constructs a new InvalidStatusTransitionException with the specified detail message.
     *
     * @param message the detail message explaining the invalid transition
     */
    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}