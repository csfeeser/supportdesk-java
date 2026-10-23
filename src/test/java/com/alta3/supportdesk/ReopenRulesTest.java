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
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for the rule that reopening a CLOSED ticket needs a reason.
 * Sample tickets 3 and 5 are CLOSED; tickets 1 and 4 are OPEN.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReopenRulesTest {

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

    private ResultActions patchStatus(long id, String json) throws Exception {
        return mockMvc.perform(patch("/api/tickets/" + id + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private void assertTicketStillClosedAndNeverReopened(long id) throws Exception {
        mockMvc.perform(get("/api/tickets/" + id))
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.lastReopenReason").value(nullValue()));
    }

    @Test
    void lastReopenReasonIsNullForTicketsThatWereNeverReopened() throws Exception {
        mockMvc.perform(get("/api/tickets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastReopenReason").value(nullValue()));
    }

    @Test
    void reopenToOpenWithValidReason() throws Exception {
        patchStatus(3, """
                {"status": "OPEN", "reason": "Customer reports the issue has returned"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.lastReopenReason").value("Customer reports the issue has returned"));

        mockMvc.perform(get("/api/tickets/3"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.lastReopenReason").value("Customer reports the issue has returned"));
    }

    @Test
    void reopenToInProgressWithValidReason() throws Exception {
        patchStatus(5, """
                {"status": "IN_PROGRESS", "reason": "Billing team confirmed it is still wrong"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.lastReopenReason").value("Billing team confirmed it is still wrong"));
    }

    @Test
    void reasonIsTrimmedWhenStored() throws Exception {
        patchStatus(3, """
                {"status": "OPEN", "reason": "   It happened again today   "}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastReopenReason").value("It happened again today"));
    }

    @Test
    void reopeningWithoutReasonIsRejected() throws Exception {
        patchStatus(3, """
                {"status": "OPEN"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("requires a reason")));

        assertTicketStillClosedAndNeverReopened(3);
    }

    @Test
    void reopeningWithEmptyReasonIsRejected() throws Exception {
        patchStatus(3, """
                {"status": "IN_PROGRESS", "reason": ""}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        assertTicketStillClosedAndNeverReopened(3);
    }

    @Test
    void reopeningWithTooShortReasonIsRejected() throws Exception {
        patchStatus(3, """
                {"status": "OPEN", "reason": "too short"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("at least 10 characters")));

        assertTicketStillClosedAndNeverReopened(3);
    }

    @Test
    void reopeningWithWhitespaceOnlyReasonIsRejected() throws Exception {
        patchStatus(3, """
                {"status": "OPEN", "reason": "          "}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("requires a reason")));

        assertTicketStillClosedAndNeverReopened(3);
    }

    @Test
    void closingATicketDoesNotNeedAReason() throws Exception {
        patchStatus(4, """
                {"status": "CLOSED"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.lastReopenReason").value(nullValue()));
    }

    @Test
    void ticketsThatAreNotClosedNeverNeedAReason() throws Exception {
        // Ticket 2 is IN_PROGRESS: moving back to OPEN is not a reopen.
        patchStatus(2, """
                {"status": "OPEN"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.lastReopenReason").value(nullValue()));
    }
}
