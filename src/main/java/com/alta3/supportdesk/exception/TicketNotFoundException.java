package com.alta3.supportdesk.exception;

public class TicketNotFoundException extends RuntimeException {

    public TicketNotFoundException(Long id) {
        super("Ticket " + id + " was not found");
    }
}
