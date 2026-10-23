package com.alta3.supportdesk.service;

import com.alta3.supportdesk.exception.InvalidRequestException;
import com.alta3.supportdesk.exception.TicketNotFoundException;
import com.alta3.supportdesk.model.Ticket;
import com.alta3.supportdesk.model.TicketStatus;
import com.alta3.supportdesk.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class TicketService {

    private static final int TITLE_MIN = 3;
    private static final int TITLE_MAX = 100;
    private static final int DESCRIPTION_MIN = 1;
    private static final int DESCRIPTION_MAX = 500;
    private static final int ASSIGNEE_MAX = 60;

    private final TicketRepository repository;

    public TicketService(TicketRepository repository) {
        this.repository = repository;
    }

    public List<Ticket> getAllTickets() {
        return repository.findAll();
    }

    public Ticket getTicket(Long id) {
        return repository.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
    }

    public Ticket createTicket(String title, String description, String assignee) {
        String cleanTitle = validateText("title", title, TITLE_MIN, TITLE_MAX);
        String cleanDescription = validateText("description", description, DESCRIPTION_MIN, DESCRIPTION_MAX);
        String cleanAssignee = validateAssignee(assignee);

        Ticket ticket = new Ticket(cleanTitle, cleanDescription, TicketStatus.OPEN, cleanAssignee);
        return repository.save(ticket);
    }

    public Ticket updateStatus(Long id, String status) {
        TicketStatus newStatus = parseStatus(status);
        Ticket ticket = getTicket(id);
        ticket.setStatus(newStatus);
        return repository.save(ticket);
    }

    public Ticket updateAssignee(Long id, String assignee) {
        String cleanAssignee = validateAssignee(assignee);
        Ticket ticket = getTicket(id);
        ticket.setAssignee(cleanAssignee);
        return repository.save(ticket);
    }

    private String validateText(String field, String value, int min, int max) {
        if (value == null) {
            throw new InvalidRequestException("The " + field + " is required");
        }
        String trimmed = value.trim();
        if (trimmed.length() < min || trimmed.length() > max) {
            throw new InvalidRequestException(
                    "The " + field + " must be between " + min + " and " + max + " characters");
        }
        return trimmed;
    }

    /** A null assignee means "unassigned". Anything else must be 1-60 characters after trimming. */
    private String validateAssignee(String assignee) {
        if (assignee == null) {
            return null;
        }
        return validateText("assignee", assignee, 1, ASSIGNEE_MAX);
    }

    private TicketStatus parseStatus(String status) {
        if (status == null) {
            throw new InvalidRequestException("The status is required");
        }
        try {
            return TicketStatus.valueOf(status.trim());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(
                    "Invalid status '" + status + "'. Allowed values: " + Arrays.toString(TicketStatus.values()));
        }
    }
}
