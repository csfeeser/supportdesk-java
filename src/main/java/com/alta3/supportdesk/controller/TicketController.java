package com.alta3.supportdesk.controller;

import com.alta3.supportdesk.dto.CreateTicketRequest;
import com.alta3.supportdesk.dto.UpdateAssigneeRequest;
import com.alta3.supportdesk.dto.UpdateStatusRequest;
import com.alta3.supportdesk.model.Ticket;
import com.alta3.supportdesk.service.TicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public List<Ticket> listTickets() {
        return ticketService.getAllTickets();
    }

    @GetMapping("/{id}")
    public Ticket getTicket(@PathVariable Long id) {
        return ticketService.getTicket(id);
    }

    @PostMapping
    public ResponseEntity<Ticket> createTicket(@RequestBody CreateTicketRequest request) {
        Ticket created = ticketService.createTicket(request.title(), request.description(), request.assignee());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping("/{id}/status")
    public Ticket updateStatus(@PathVariable Long id, @RequestBody UpdateStatusRequest request) {
        return ticketService.updateStatus(id, request.status());
    }

    @PatchMapping("/{id}/assignee")
    public Ticket updateAssignee(@PathVariable Long id, @RequestBody UpdateAssigneeRequest request) {
        return ticketService.updateAssignee(id, request.assignee());
    }
}
