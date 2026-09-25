package com.example.supportticket.controller;

import com.example.supportticket.dto.*;
import com.example.supportticket.entity.Ticket;
import com.example.supportticket.entity.Comment;
import com.example.supportticket.exception.InvalidStatusTransitionException;
import com.example.supportticket.exception.TicketNotFoundException;
import com.example.supportticket.service.TicketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TicketController.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TicketService ticketService;

    @Autowired
    private ObjectMapper objectMapper;

    // Utility for Ticket entity creation
    private Ticket sampleTicket(Long id) {
        Ticket ticket = new Ticket();
        ticket.setId(id);
        ticket.setTitle("Sample Title");
        ticket.setDescription("Sample Description");
        ticket.setStatus(Ticket.Status.OPEN);
        ticket.setPriority(Ticket.Priority.HIGH);
        ticket.setAssignee("johndoe");
        ticket.setCreatedAt(java.time.LocalDateTime.of(2024, 4, 1, 12, 0));
        ticket.setUpdatedAt(java.time.LocalDateTime.of(2024, 4, 1, 13, 0));
        return ticket;
    }

    // Mockito-based utility for Comment entity creation with getId(), getBody(), and getCreatedAt() mocked.
    private Comment sampleComment(Long id) {
        Comment mockComment = mock(Comment.class);
        when(mockComment.getId()).thenReturn(id);
        when(mockComment.getBody()).thenReturn("A comment");
        when(mockComment.getCreatedAt())
                .thenReturn(java.time.LocalDateTime.of(2024, 4, 1, 13, 30));
        return mockComment;
    }

    @Nested
    @DisplayName("POST /api/v1/tickets")
    class CreateTicketTests {
        @Test
        @DisplayName("valid request returns 201 and response contains main fields and Location header")
        void createTicket_valid() throws Exception {
            CreateTicketRequest req = new CreateTicketRequest();
            req.setTitle("The Bug");
            req.setDescription("Serious issue");
            req.setPriority("HIGH");
            req.setAssignee("jane");

            Ticket ticket = sampleTicket(123L);

            when(ticketService.createTicket("The Bug", "Serious issue", "HIGH", "jane")).thenReturn(ticket);
            when(ticketService.getCommentsForTicket(123L)).thenReturn(Collections.emptyList());

            mockMvc.perform(post("/api/v1/tickets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("/api/v1/tickets/123")))
                    .andExpect(jsonPath("$.ticketId").value(123L))
                    .andExpect(jsonPath("$.title").value("Sample Title"))
                    .andExpect(jsonPath("$.description").value("Sample Description"))
                    .andExpect(jsonPath("$.status").value("OPEN"))
                    .andExpect(jsonPath("$.priority").value("HIGH"))
                    .andExpect(jsonPath("$.assignee").value("johndoe"))
            ;
        }

        @Test
        @DisplayName("missing required field: title yields 400")
        void createTicket_missingTitle() throws Exception {
            CreateTicketRequest req = new CreateTicketRequest();
            req.setDescription("desc");
            req.setPriority("LOW");
            req.setAssignee("bob");

            mockMvc.perform(post("/api/v1/tickets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.message", containsString("title")));
        }

        @Test
        @DisplayName("empty request yields 400")
        void createTicket_emptyBody() throws Exception {
            mockMvc.perform(post("/api/v1/tickets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("malformed JSON yields 400")
        void createTicket_malformedJson() throws Exception {
            String badJson = "{\"title\": \"hello\", "; // trailing comma
            mockMvc.perform(post("/api/v1/tickets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(badJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/tickets")
    class ListTicketsTests {
        @Test
        @DisplayName("returns 200 and array of tickets")
        void listAllTickets() throws Exception {
            List<Ticket> tickets = List.of(sampleTicket(1L), sampleTicket(2L));
            when(ticketService.searchTickets(null, null)).thenReturn(tickets);

            mockMvc.perform(get("/api/v1/tickets"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].ticketId").value(1L))
                    .andExpect(jsonPath("$[1].ticketId").value(2L));
        }

        @Test
        @DisplayName("returns 200, supports 'q' parameter")
        void listTicketsWithQ() throws Exception {
            List<Ticket> tickets = List.of(sampleTicket(3L));
            when(ticketService.searchTickets(null, "bug")).thenReturn(tickets);

            mockMvc.perform(get("/api/v1/tickets")
                    .param("q", "bug"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].ticketId").value(3L));
        }

        @Test
        @DisplayName("supports 'status' parameter")
        void listTicketsWithStatus() throws Exception {
            List<Ticket> tickets = List.of(sampleTicket(5L));
            when(ticketService.searchTickets(Ticket.Status.OPEN, null)).thenReturn(tickets);

            mockMvc.perform(get("/api/v1/tickets")
                    .param("status", "open"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].ticketId").value(5L));
        }

        @Test
        @DisplayName("supports both 'q' and 'status' param")
        void listTicketsWithQAndStatus() throws Exception {
            List<Ticket> tickets = List.of(sampleTicket(7L));
            when(ticketService.searchTickets(Ticket.Status.CLOSED, "network")).thenReturn(tickets);

            mockMvc.perform(get("/api/v1/tickets")
                    .param("q", "network")
                    .param("status", "closed"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].ticketId").value(7L));
        }

        @Test
        @DisplayName("invalid status returns 400 with MALFORMED_REQUEST")
        void listTickets_invalidStatus() throws Exception {
            mockMvc.perform(get("/api/v1/tickets")
                    .param("status", "NOTASTATUS"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("MALFORMED_REQUEST")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/tickets/{ticketId}")
    class GetTicketById {
        @Test
        @DisplayName("existing ticket returns 200")
        void getTicketById_found() throws Exception {
            Ticket t = sampleTicket(100L);
            List<Comment> comments = List.of(sampleComment(888L));

            when(ticketService.getTicketById(100L)).thenReturn(t);
            when(ticketService.getCommentsForTicket(100L)).thenReturn(comments);

            mockMvc.perform(get("/api/v1/tickets/100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ticketId").value(100L))
                    .andExpect(jsonPath("$.comments", hasSize(1)))
                    .andExpect(jsonPath("$.comments[0].commentId").value(888L));
        }

        @Test
        @DisplayName("missing ticket returns 404 with code TICKET_NOT_FOUND")
        void getTicketById_notFound() throws Exception {
            when(ticketService.getTicketById(777L)).thenThrow(new TicketNotFoundException("Not found"));

            mockMvc.perform(get("/api/v1/tickets/777"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("Not found"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/tickets/{ticketId}")
    class UpdateTicket {
        @Test
        @DisplayName("valid update returns 200")
        void updateTicket_valid() throws Exception {
            UpdateTicketRequest req = new UpdateTicketRequest();
            req.setTitle("New Title");
            req.setDescription("New Description");
            req.setPriority("LOW");

            Ticket updated = sampleTicket(10L);
            updated.setTitle("New Title");
            updated.setDescription("New Description");
            updated.setPriority(Ticket.Priority.LOW);

            when(ticketService.updateTicketDetails(10L, "New Title", "New Description", "LOW"))
                    .thenReturn(updated);

            mockMvc.perform(patch("/api/v1/tickets/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("New Title"))
                    .andExpect(jsonPath("$.priority").value("LOW"));
        }

        @Test
        @DisplayName("missing required fields returns 400")
        void updateTicket_missingFields() throws Exception {
            UpdateTicketRequest req = new UpdateTicketRequest();
            // all fields null

            mockMvc.perform(patch("/api/v1/tickets/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("malformed JSON produces 400")
        void updateTicket_malformedJson() throws Exception {
            String badJson = "{\"title\" \"oops\"}";
            mockMvc.perform(patch("/api/v1/tickets/11")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(badJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/tickets/{ticketId}/status")
    class UpdateStatus {
        @Test
        @DisplayName("valid status transition returns 200")
        void updateStatus_valid() throws Exception {
            UpdateStatusRequest req = new UpdateStatusRequest();
            req.setStatus("CLOSED");

            Ticket ticket = sampleTicket(47L);
            ticket.setStatus(Ticket.Status.CLOSED);

            when(ticketService.transitionStatus(47L, Ticket.Status.CLOSED)).thenReturn(ticket);

            mockMvc.perform(patch("/api/v1/tickets/47/status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CLOSED"));
        }

        @Test
        @DisplayName("invalid status value returns 400")
        void updateStatus_invalidValue() throws Exception {
            UpdateStatusRequest req = new UpdateStatusRequest();
            req.setStatus("BADSTATUS");

            mockMvc.perform(patch("/api/v1/tickets/2/status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        }

        @Test
        @DisplayName("InvalidStatusTransitionException returns 409 INVALID_STATUS_TRANSITION")
        void updateStatus_transitionConflict() throws Exception {
            UpdateStatusRequest req = new UpdateStatusRequest();
            req.setStatus("IN_PROGRESS");

            doThrow(new InvalidStatusTransitionException("Bad transition"))
                    .when(ticketService).transitionStatus(eq(1L), eq(Ticket.Status.IN_PROGRESS));

            mockMvc.perform(patch("/api/v1/tickets/1/status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"))
                    .andExpect(jsonPath("$.message").value("Bad transition"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/tickets/{ticketId}/assignee")
    class PatchAssignee {
        @Test
        @DisplayName("valid assignee returns 200")
        void setAssignee_valid() throws Exception {
            AssigneeRequest req = new AssigneeRequest();
            req.setAssignee("alex");

            Ticket ticket = sampleTicket(12L);
            ticket.setAssignee("alex");

            when(ticketService.setAssignee(12L, "alex")).thenReturn(ticket);

            mockMvc.perform(patch("/api/v1/tickets/12/assignee")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.assignee").value("alex"));
        }

        @Test
        @DisplayName("null assignee is accepted")
        void setAssignee_null() throws Exception {
            AssigneeRequest req = new AssigneeRequest();
            req.setAssignee(null);

            Ticket ticket = sampleTicket(14L);
            ticket.setAssignee(null);

            when(ticketService.setAssignee(14L, null)).thenReturn(ticket);

            mockMvc.perform(patch("/api/v1/tickets/14/assignee")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.assignee").value(nullValue()));
        }

        @Test
        @DisplayName("blank assignee returns 400")
        void setAssignee_blank() throws Exception {
            AssigneeRequest req = new AssigneeRequest();
            req.setAssignee("   ");

            mockMvc.perform(patch("/api/v1/tickets/16/assignee")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.message", containsString("Assignee must not be blank")));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/tickets/{ticketId}/comments")
    class AddComment {
        @Test
        @DisplayName("valid comment returns 201")
        void addComment_valid() throws Exception {
            AddCommentRequest req = new AddCommentRequest();
            req.setBody("a good comment");

            Comment comment = sampleComment(555L);
            when(ticketService.addComment(101L, "a good comment")).thenReturn(comment);

            mockMvc.perform(post("/api/v1/tickets/101/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.commentId").value(555L))
                    .andExpect(jsonPath("$.body").value("A comment"))
                    .andExpect(jsonPath("$.createdAt").exists());
        }

        @Test
        @DisplayName("blank comment body returns 400")
        void addComment_blankBody() throws Exception {
            AddCommentRequest req = new AddCommentRequest();
            req.setBody("    ");

            mockMvc.perform(post("/api/v1/tickets/99/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }
}