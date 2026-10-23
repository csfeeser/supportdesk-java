package com.alta3.supportdesk.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"id", "title", "description", "status", "assignee", "priority"})
public class Ticket {

    private Long id;
    private String title;
    private String description;
    private TicketStatus status;
    private String assignee;
    private TicketPriority priority;

    public Ticket(String title, String description, TicketStatus status, String assignee,
                  TicketPriority priority) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.assignee = assignee;
        this.priority = priority;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }
}
