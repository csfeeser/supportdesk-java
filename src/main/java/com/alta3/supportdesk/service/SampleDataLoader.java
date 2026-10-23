package com.alta3.supportdesk.service;

import com.alta3.supportdesk.model.Ticket;
import com.alta3.supportdesk.model.TicketStatus;
import com.alta3.supportdesk.repository.TicketRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Loads the six sample tickets (IDs 1-6) every time the application starts. */
@Component
public class SampleDataLoader implements CommandLineRunner {

    private final TicketRepository repository;

    public SampleDataLoader(TicketRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        add("Cannot log in", "Login fails with an error after entering valid credentials",
                TicketStatus.OPEN, "alex");
        add("Invoice export fails", "Exporting invoices to CSV stops with a timeout",
                TicketStatus.IN_PROGRESS, "alex");
        add("Search results are incorrect", "Searching for a customer name returns unrelated records",
                TicketStatus.CLOSED, "morgan");
        add("Password reset email missing", "The password reset email never arrives in the inbox",
                TicketStatus.OPEN, "morgan");
        add("Billing address incorrect", "Invoices show an outdated billing address",
                TicketStatus.CLOSED, null);
        add("Dashboard loads slowly", "The main dashboard takes more than 30 seconds to load",
                TicketStatus.IN_PROGRESS, null);
    }

    private void add(String title, String description, TicketStatus status, String assignee) {
        repository.save(new Ticket(title, description, status, assignee));
    }
}
