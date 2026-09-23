package com.example.supportticket.exception;

/**
 * Exception thrown when a requested ticket is not found.
 */
public class TicketNotFoundException extends RuntimeException {

    public TicketNotFoundException(String message) {
        super(message);
    }
}