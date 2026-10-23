package com.alta3.supportdesk;

import com.alta3.supportdesk.repository.TicketRepository;
import com.alta3.supportdesk.service.SampleDataLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for filtering GET /api/tickets. These use the six sample tickets:
 * 1 OPEN alex, 2 IN_PROGRESS alex, 3 CLOSED morgan, 4 OPEN morgan, 5 CLOSED unassigned,
 * 6 IN_PROGRESS unassigned.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TicketFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TicketRepository repository;

    @Autowired
    private SampleDataLoader sampleDataLoader;

    @BeforeEach
    void resetTicketData() {
        repository.deleteAll();
        sampleDataLoader.run();
    }

    @Test
    void noFiltersReturnsAllTicketsInIdOrder() throws Exception {
        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(1, 2, 3, 4, 5, 6)));
    }

    @Test
    void filterByStatus() throws Exception {
        mockMvc.perform(get("/api/tickets").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(1, 4)));
    }

    @Test
    void filterByAssignee() throws Exception {
        mockMvc.perform(get("/api/tickets").param("assignee", "alex"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(1, 2)));
    }

    @Test
    void filterByStatusAndAssigneeRequiresBothToMatch() throws Exception {
        mockMvc.perform(get("/api/tickets")
                        .param("status", "OPEN")
                        .param("assignee", "alex"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(1)));
    }

    @Test
    void statusFilterIsCaseInsensitive() throws Exception {
        mockMvc.perform(get("/api/tickets").param("status", "in_progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(2, 6)));
    }

    @Test
    void assigneeFilterIsCaseInsensitiveAndIgnoresSurroundingSpaces() throws Exception {
        mockMvc.perform(get("/api/tickets").param("assignee", "  MORGAN "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(3, 4)));
    }

    @Test
    void invalidStatusIsRejected() throws Exception {
        mockMvc.perform(get("/api/tickets").param("status", "DONE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("Invalid status")));
    }

    @Test
    void noMatchesReturnsEmptyArray() throws Exception {
        mockMvc.perform(get("/api/tickets").param("assignee", "nobody"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
