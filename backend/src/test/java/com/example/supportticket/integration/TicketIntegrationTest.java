package com.example.supportticket.integration;

import com.example.supportticket.entity.Comment;
import com.example.supportticket.entity.Ticket;
import com.example.supportticket.entity.Ticket.Status;
import com.example.supportticket.exception.InvalidStatusTransitionException;
import com.example.supportticket.exception.TicketNotFoundException;
import com.example.supportticket.repository.CommentRepository;
import com.example.supportticket.service.TicketService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class TicketIntegrationTest {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private EntityManager entityManager;

    private Ticket createTicket(String assignee) {
        return ticketService.createTicket("Title", "Desc", "HIGH", assignee);
    }

    @Test
    @DisplayName("Create and fetch ticket by ID")
    void createAndFetchTicket() {
        Ticket created = ticketService.createTicket("T1", "Description", "MEDIUM", "alice");
        assertThat(created.getId()).isNotNull();

        Ticket loaded = ticketService.getTicketById(created.getId());
        assertThat(loaded.getTitle()).isEqualTo("T1");
        assertThat(loaded.getDescription()).isEqualTo("Description");
        assertThat(loaded.getPriority().name()).isEqualTo("MEDIUM");
        assertThat(loaded.getAssignee()).isEqualTo("alice");
        assertThat(loaded.getStatus()).isEqualTo(Status.OPEN);
    }

    @Test
    @DisplayName("Update and verify persistence after flush/clear")
    void updateAndPersistence() {
        Ticket ticket = createTicket("bob");
        Long id = ticket.getId();
        ticketService.updateTicketDetails(id, "Updated", "New Desc", "LOW");
        entityManager.flush();
        entityManager.clear();
        Ticket updated = ticketService.getTicketById(id);
        assertThat(updated.getTitle()).isEqualTo("Updated");
        assertThat(updated.getDescription()).isEqualTo("New Desc");
        assertThat(updated.getPriority().name()).isEqualTo("LOW");
    }

    @Test
    @DisplayName("Add comment and verify association")
    void addCommentAndVerify() {
        Ticket ticket = createTicket("alice");
        ticketService.addComment(ticket.getId(), "hello world");
        List<Comment> comments = commentRepository.findByTicketOrderByCreatedAtAsc(ticket);
        assertThat(comments).hasSize(1);
        Comment comment = comments.get(0);
        assertThat(comment.getBody()).isEqualTo("hello world");
        assertThat(comment.getTicket().getId()).isEqualTo(ticket.getId());
    }

    @Test
    @DisplayName("Valid status flow: OPEN → IN_PROGRESS → RESOLVED → CLOSED")
    void validStatusFlow() {
        Ticket ticket = createTicket("bob");
        Long id = ticket.getId();

        ticketService.transitionStatus(id, Status.IN_PROGRESS);
        assertThat(ticketService.getTicketById(id).getStatus()).isEqualTo(Status.IN_PROGRESS);

        ticketService.transitionStatus(id, Status.RESOLVED);
        assertThat(ticketService.getTicketById(id).getStatus()).isEqualTo(Status.RESOLVED);

        ticketService.transitionStatus(id, Status.CLOSED);
        assertThat(ticketService.getTicketById(id).getStatus()).isEqualTo(Status.CLOSED);
    }

    @Test
    @DisplayName("Valid cancellation: OPEN→CANCELLED and IN_PROGRESS→CANCELLED")
    void validCancellation() {
        Ticket open = createTicket("u1");
        ticketService.transitionStatus(open.getId(), Status.CANCELLED);
        assertThat(ticketService.getTicketById(open.getId()).getStatus()).isEqualTo(Status.CANCELLED);

        Ticket inProgress = createTicket("u2");
        ticketService.transitionStatus(inProgress.getId(), Status.IN_PROGRESS);
        ticketService.transitionStatus(inProgress.getId(), Status.CANCELLED);
        assertThat(ticketService.getTicketById(inProgress.getId()).getStatus()).isEqualTo(Status.CANCELLED);
    }

    @Nested
    class InvalidStateTransitions {

        @Test
        @DisplayName("Invalid transitions are rejected")
        void invalidTransitionsAreRejected() {
            Ticket t1 = createTicket("a1");
            // OPEN: can't go to RESOLVED, CLOSED
            assertThatThrownBy(() -> ticketService.transitionStatus(t1.getId(), Status.RESOLVED))
                .isInstanceOf(InvalidStatusTransitionException.class);
            assertThatThrownBy(() -> ticketService.transitionStatus(t1.getId(), Status.CLOSED))
                .isInstanceOf(InvalidStatusTransitionException.class);

            // IN_PROGRESS: can't go to OPEN, CLOSED
            Ticket t2 = createTicket("a2");
            ticketService.transitionStatus(t2.getId(), Status.IN_PROGRESS);
            assertThatThrownBy(() -> ticketService.transitionStatus(t2.getId(), Status.OPEN))
                .isInstanceOf(InvalidStatusTransitionException.class);
            assertThatThrownBy(() -> ticketService.transitionStatus(t2.getId(), Status.CLOSED))
                .isInstanceOf(InvalidStatusTransitionException.class);

            // RESOLVED: can't go to any except CLOSED
            Ticket t3 = createTicket("a3");
            ticketService.transitionStatus(t3.getId(), Status.IN_PROGRESS);
            ticketService.transitionStatus(t3.getId(), Status.RESOLVED);
            assertThatThrownBy(() -> ticketService.transitionStatus(t3.getId(), Status.OPEN))
                .isInstanceOf(InvalidStatusTransitionException.class);
            assertThatThrownBy(() -> ticketService.transitionStatus(t3.getId(), Status.IN_PROGRESS))
                .isInstanceOf(InvalidStatusTransitionException.class);
            assertThatThrownBy(() -> ticketService.transitionStatus(t3.getId(), Status.CANCELLED))
                .isInstanceOf(InvalidStatusTransitionException.class);

            // Same-status rejection for all enums
            for (Status status : Status.values()) {
                Ticket t = createTicket("same-" + status);
                // Bring ticket to the status if possible
                switch (status) {
                    case IN_PROGRESS:
                        ticketService.transitionStatus(t.getId(), Status.IN_PROGRESS);
                        break;
                    case RESOLVED:
                        ticketService.transitionStatus(t.getId(), Status.IN_PROGRESS);
                        ticketService.transitionStatus(t.getId(), Status.RESOLVED);
                        break;
                    case CLOSED:
                        ticketService.transitionStatus(t.getId(), Status.IN_PROGRESS);
                        ticketService.transitionStatus(t.getId(), Status.RESOLVED);
                        ticketService.transitionStatus(t.getId(), Status.CLOSED);
                        break;
                    case CANCELLED:
                        ticketService.transitionStatus(t.getId(), Status.CANCELLED);
                        break;
                    default:
                        break;
                }
                Status target = status;
                assertThatThrownBy(() -> ticketService.transitionStatus(t.getId(), target))
                    .isInstanceOf(InvalidStatusTransitionException.class);
            }

            // Terminal-state rejection (CLOSED)
            Ticket tClosed = createTicket("c-closed");
            ticketService.transitionStatus(tClosed.getId(), Status.IN_PROGRESS);
            ticketService.transitionStatus(tClosed.getId(), Status.RESOLVED);
            ticketService.transitionStatus(tClosed.getId(), Status.CLOSED);
            for (Status s : Status.values()) {
                if (s != Status.CLOSED) {
                    assertThatThrownBy(() -> ticketService.transitionStatus(tClosed.getId(), s))
                        .isInstanceOf(InvalidStatusTransitionException.class);
                }
            }

            // Terminal-state rejection (CANCELLED)
            Ticket tCancelled = createTicket("c-cancelled");
            ticketService.transitionStatus(tCancelled.getId(), Status.CANCELLED);
            for (Status s : Status.values()) {
                if (s != Status.CANCELLED) {
                    assertThatThrownBy(() -> ticketService.transitionStatus(tCancelled.getId(), s))
                        .isInstanceOf(InvalidStatusTransitionException.class);
                }
            }
        }
    }

    @Test
    @DisplayName("TicketNotFoundException for non-existent tickets")
    void ticketNotFound() {
        Long invalidId = -12345L;
        assertThatThrownBy(() -> ticketService.getTicketById(invalidId))
            .isInstanceOf(TicketNotFoundException.class);
        assertThatThrownBy(() -> ticketService.updateTicketDetails(invalidId, "a", "b", "LOW"))
            .isInstanceOf(TicketNotFoundException.class);
        assertThatThrownBy(() -> ticketService.transitionStatus(invalidId, Status.RESOLVED))
            .isInstanceOf(TicketNotFoundException.class);
        assertThatThrownBy(() -> ticketService.addComment(invalidId, "fail"))
            .isInstanceOf(TicketNotFoundException.class);
    }

    @Test
    @DisplayName("Persistence verified after flush/clear (ticket & comment)")
    void persistenceAfterFlushClear() {
        Ticket ticket = ticketService.createTicket("Persistent", "persist desc", "MEDIUM", "eve");
        ticketService.addComment(ticket.getId(), "abc comment");
        entityManager.flush();
        entityManager.clear();
        Ticket refreshed = ticketService.getTicketById(ticket.getId());
        assertThat(refreshed.getTitle()).isEqualTo("Persistent");
        assertThat(refreshed.getAssignee()).isEqualTo("eve");
        List<Comment> comments = commentRepository.findByTicketOrderByCreatedAtAsc(refreshed);
        assertThat(comments).hasSize(1)
                            .extracting(Comment::getBody)
                            .containsExactly("abc comment");
    }
}