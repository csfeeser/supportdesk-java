package com.alta3.supportdesk;

import com.alta3.supportdesk.repository.TicketRepository;
import com.alta3.supportdesk.service.SampleDataLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tests for ticket priorities. Each test starts with the six sample tickets (all MEDIUM). */
@SpringBootTest
@AutoConfigureMockMvc
class TicketPriorityTest {

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

    private ResultActions createTicket(String title, String priority) throws Exception {
        String priorityJson = priority == null ? "" : ", \"priority\": \"" + priority + "\"";
        return mockMvc.perform(post("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"" + title + "\", \"description\": \"Some details\"" + priorityJson + "}"));
    }

    @Test
    void ticketsDefaultToMediumPriority() throws Exception {
        createTicket("No priority given", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.priority").value("MEDIUM"));

        mockMvc.perform(get("/api/tickets/1"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"));
    }

    @Test
    void createTicketWithPriority() throws Exception {
        createTicket("Production server unavailable", "URGENT")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.priority").value("URGENT"));
    }

    @Test
    void rejectInvalidPriority() throws Exception {
        createTicket("Bad priority", "CRITICAL")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("Invalid priority 'CRITICAL'")));

        mockMvc.perform(get("/api/tickets"))
                .andExpect(jsonPath("$.length()").value(6));
    }

    @Test
    void listWithoutSortStaysInIdOrder() throws Exception {
        createTicket("Low one", "LOW");
        createTicket("Urgent one", "URGENT");

        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(1, 2, 3, 4, 5, 6, 7, 8)));
    }

    @Test
    void ticketsWithEqualPriorityAreSortedById() throws Exception {
        // All six sample tickets are MEDIUM, so priority order is just ID order.
        mockMvc.perform(get("/api/tickets").param("sort", "priority"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(1, 2, 3, 4, 5, 6)));
    }

    @Test
    void sortByPriorityListsMostUrgentFirst() throws Exception {
        createTicket("Low priority", "LOW");       // id 7
        createTicket("Urgent one", "URGENT");      // id 8
        createTicket("High priority", "HIGH");     // id 9
        createTicket("Urgent two", "URGENT");      // id 10

        // URGENT, then HIGH, then MEDIUM (sample tickets), then LOW. Ties by ascending ID.
        mockMvc.perform(get("/api/tickets").param("sort", "priority"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(8, 10, 9, 1, 2, 3, 4, 5, 6, 7)));
    }

    @Test
    void rejectUnsupportedSort() throws Exception {
        mockMvc.perform(get("/api/tickets").param("sort", "title"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("Unsupported sort 'title'")));
    }
}
