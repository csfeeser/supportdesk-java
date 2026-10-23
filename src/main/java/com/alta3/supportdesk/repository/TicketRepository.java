package com.alta3.supportdesk.repository;

import com.alta3.supportdesk.model.Ticket;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class TicketRepository {

    private final Map<Long, Ticket> tickets = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    /** Stores the ticket. A ticket without an ID is given the next unique ID. */
    public Ticket save(Ticket ticket) {
        if (ticket.getId() == null) {
            ticket.setId(nextId.getAndIncrement());
        }
        tickets.put(ticket.getId(), ticket);
        return ticket;
    }

    public Optional<Ticket> findById(Long id) {
        return Optional.ofNullable(tickets.get(id));
    }

    /** Returns all tickets in ascending ID order. */
    public List<Ticket> findAll() {
        return tickets.values().stream()
                .sorted(Comparator.comparing(Ticket::getId))
                .toList();
    }

    /** Removes all tickets and starts ID numbering again at 1. */
    public void deleteAll() {
        tickets.clear();
        nextId.set(1);
    }
}
