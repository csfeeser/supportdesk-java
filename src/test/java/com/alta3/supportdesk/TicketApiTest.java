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
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API tests. The ticket data is reset before each test, so every test starts
 * with exactly the six sample tickets and tests never depend on each other.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TicketApiTest {

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

    // ---- helpers ----

    private ResultActions postTicket(String json) throws Exception {
        return mockMvc.perform(post("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private ResultActions patchStatus(long id, String json) throws Exception {
        return mockMvc.perform(patch("/api/tickets/" + id + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private ResultActions patchAssignee(long id, String json) throws Exception {
        return mockMvc.perform(patch("/api/tickets/" + id + "/assignee")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    // ---- health and sample data ----

    @Test
    void healthReportsUp() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void sampleDataIsLoadedAtStartup() throws Exception {
        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].title").value("Cannot log in"))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].assignee").value("alex"))
                .andExpect(jsonPath("$[4].title").value("Billing address incorrect"))
                .andExpect(jsonPath("$[4].status").value("CLOSED"))
                .andExpect(jsonPath("$[4].assignee").value(nullValue()))
                .andExpect(jsonPath("$[5].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[5].assignee").value(nullValue()));
    }

    // ---- creating tickets ----

    @Test
    void createValidTicket() throws Exception {
        postTicket("""
                {"title": "Printer not working",
                 "description": "The office printer is displaying an error",
                 "assignee": "alex"}""")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/tickets/7")))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.title").value("Printer not working"))
                .andExpect(jsonPath("$.description").value("The office printer is displaying an error"))
                .andExpect(jsonPath("$.assignee").value("alex"));
    }

    @Test
    void createdTicketIsOpenEvenWhenNoAssigneeIsGiven() throws Exception {
        postTicket("""
                {"title": "Network is down", "description": "No connection"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.assignee").value(nullValue()));
    }

    @Test
    void createdTicketsGetUniqueIncreasingIds() throws Exception {
        String ticket = """
                {"title": "Same title", "description": "Same description"}""";
        postTicket(ticket).andExpect(jsonPath("$.id").value(7));
        postTicket(ticket).andExpect(jsonPath("$.id").value(8));
        postTicket(ticket).andExpect(jsonPath("$.id").value(9));
    }

    @Test
    void createTrimsWhitespaceFromFields() throws Exception {
        postTicket("""
                {"title": "  Spaced out  ", "description": "  details  ", "assignee": "  alex  "}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Spaced out"))
                .andExpect(jsonPath("$.description").value("details"))
                .andExpect(jsonPath("$.assignee").value("alex"));
    }

    @Test
    void rejectTitleThatIsTooShort() throws Exception {
        postTicket("""
                {"title": "ab", "description": "Some description"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("title")));
    }

    @Test
    void rejectTitleThatIsOnlyWhitespace() throws Exception {
        postTicket("""
                {"title": "      ", "description": "Some description"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectTitleThatIsTooLong() throws Exception {
        postTicket("{\"title\": \"" + "x".repeat(101) + "\", \"description\": \"Some description\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("title")));
    }

    @Test
    void rejectMissingTitle() throws Exception {
        postTicket("""
                {"description": "Some description"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The title is required"));
    }

    @Test
    void rejectBlankDescription() throws Exception {
        postTicket("""
                {"title": "Valid title", "description": "   "}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("description")));
    }

    @Test
    void rejectDescriptionThatIsTooLong() throws Exception {
        postTicket("{\"title\": \"Valid title\", \"description\": \"" + "x".repeat(501) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("description")));
    }

    @Test
    void acceptTitleAndDescriptionAtTheirLimits() throws Exception {
        postTicket("{\"title\": \"" + "x".repeat(100) + "\", \"description\": \"" + "y".repeat(500) + "\"}")
                .andExpect(status().isCreated());
    }

    @Test
    void rejectBlankAssigneeOnCreate() throws Exception {
        postTicket("""
                {"title": "Valid title", "description": "Valid description", "assignee": "   "}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("assignee")));
    }

    @Test
    void rejectMalformedJson() throws Exception {
        postTicket("{ this is not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }

    @Test
    void failedCreateDoesNotConsumeAnId() throws Exception {
        postTicket("""
                {"title": "x", "description": "bad"}""").andExpect(status().isBadRequest());
        postTicket("""
                {"title": "Good title", "description": "Good description"}""")
                .andExpect(jsonPath("$.id").value(7));
    }

    // ---- retrieving tickets ----

    @Test
    void getExistingTicket() throws Exception {
        mockMvc.perform(get("/api/tickets/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.title").value("Invoice export fails"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.assignee").value("alex"));
    }

    @Test
    void getMissingTicketReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/tickets/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Ticket 99 was not found"));
    }

    @Test
    void getTicketWithNonNumericIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/tickets/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void ticketsAreListedInIdOrder() throws Exception {
        postTicket("""
                {"title": "Newest ticket", "description": "Added last"}""");

        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[2].id").value(3))
                .andExpect(jsonPath("$[3].id").value(4))
                .andExpect(jsonPath("$[4].id").value(5))
                .andExpect(jsonPath("$[5].id").value(6))
                .andExpect(jsonPath("$[6].id").value(7));
    }

    // ---- changing status ----

    @Test
    void changeStatus() throws Exception {
        patchStatus(1, """
                {"status": "IN_PROGRESS"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(get("/api/tickets/1"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void anyStatusTransitionIsAllowed() throws Exception {
        // Ticket 3 is CLOSED: reopening is allowed, with no extra conditions.
        patchStatus(3, """
                {"status": "OPEN"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
        patchStatus(3, """
                {"status": "CLOSED"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void rejectInvalidStatus() throws Exception {
        patchStatus(1, """
                {"status": "DONE"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("Invalid status 'DONE'")));

        mockMvc.perform(get("/api/tickets/1"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void rejectMissingStatus() throws Exception {
        patchStatus(1, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The status is required"));
    }

    @Test
    void changeStatusOfMissingTicketReturnsNotFound() throws Exception {
        patchStatus(99, """
                {"status": "CLOSED"}""")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Ticket 99 was not found"));
    }

    // ---- changing assignee ----

    @Test
    void assignTicketToEmployee() throws Exception {
        patchAssignee(5, """
                {"assignee": "morgan"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.assignee").value("morgan"));

        mockMvc.perform(get("/api/tickets/5"))
                .andExpect(jsonPath("$.assignee").value("morgan"));
    }

    @Test
    void unassignTicketWithNull() throws Exception {
        patchAssignee(1, """
                {"assignee": null}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee").value(nullValue()));

        mockMvc.perform(get("/api/tickets/1"))
                .andExpect(jsonPath("$.assignee").value(nullValue()));
    }

    @Test
    void rejectEmptyAssignee() throws Exception {
        patchAssignee(1, """
                {"assignee": ""}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectWhitespaceOnlyAssignee() throws Exception {
        patchAssignee(1, """
                {"assignee": "    "}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("assignee")));

        mockMvc.perform(get("/api/tickets/1"))
                .andExpect(jsonPath("$.assignee").value("alex"));
    }

    @Test
    void rejectAssigneeThatIsTooLong() throws Exception {
        patchAssignee(1, "{\"assignee\": \"" + "a".repeat(61) + "\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeAssigneeOfMissingTicketReturnsNotFound() throws Exception {
        patchAssignee(99, """
                {"assignee": "alex"}""")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }
}
