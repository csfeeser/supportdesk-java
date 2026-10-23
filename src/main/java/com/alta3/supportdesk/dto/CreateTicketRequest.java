package com.alta3.supportdesk.dto;

public record CreateTicketRequest(String title, String description, String assignee) {
}
