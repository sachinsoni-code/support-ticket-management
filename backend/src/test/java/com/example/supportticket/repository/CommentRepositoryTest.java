package com.example.supportticket.repository;

import com.example.supportticket.entity.Comment;
import com.example.supportticket.entity.Ticket;
import com.example.supportticket.entity.Ticket.Priority;
import com.example.supportticket.entity.Ticket.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private EntityManager entityManager;

    private Ticket persistTicket(
            String title,
            String description,
            Status status,
            Priority priority,
            String assignee,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        Ticket ticket = new Ticket();
        ticket.setTitle(title);
        ticket.setDescription(description);
        ticket.setStatus(status);
        ticket.setPriority(priority);
        ticket.setAssignee(assignee);
        ticket.setCreatedAt(createdAt);
        ticket.setUpdatedAt(updatedAt);
        entityManager.persist(ticket);
        entityManager.flush();
        return ticket;
    }

    // As createdAt is managed by @PrePersist, we can't set it directly.
    // We'll ensure a deterministic order by sleeping between inserts.
    private Comment persistComment(Ticket ticket, String body) {
        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setBody(body);
        entityManager.persist(comment);
        entityManager.flush();
        return comment;
    }

    // Utility to reliably get ordering by pausing (works for H2/in-memory tests)
    private void pauseMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    @Nested
    @DisplayName("findByTicketOrderByCreatedAtAsc")
    class FindByTicketOrderByCreatedAtAscTests {

        @Test
        @DisplayName("returns comments belonging to the ticket, ordered by createdAt ascending")
        void returnsOrderedCommentsForTicket() {
            LocalDateTime now = LocalDateTime.now();

            Ticket ticket = persistTicket("Ticket 1", "desc", Status.OPEN, Priority.HIGH, "alice", now, now);

            Comment c1 = persistComment(ticket, "First");
            pauseMillis(15);
            Comment c2 = persistComment(ticket, "Third");
            pauseMillis(15);
            Comment c3 = persistComment(ticket, "Second");
            pauseMillis(15);
            Comment c4 = persistComment(ticket, "Latest");

            List<Comment> result = commentRepository.findByTicketOrderByCreatedAtAsc(ticket);

            // Comments must be returned sorted by createdAt ascending (oldest first)
            assertThat(result).hasSize(4);

            // Since createdAt is set by DB, the insert order will be reflected if we pause between inserts.
            assertThat(result.get(0).getBody()).isEqualTo("First");
            assertThat(result.get(1).getBody()).isEqualTo("Third");
            assertThat(result.get(2).getBody()).isEqualTo("Second");
            assertThat(result.get(3).getBody()).isEqualTo("Latest");

            // All comments should belong to the ticket
            assertThat(result).allMatch(comment -> comment.getTicket().getId().equals(ticket.getId()));
        }

        @Test
        @DisplayName("returns empty list when ticket has no comments")
        void returnsEmptyIfNoComments() {
            Ticket ticket = persistTicket("No Comments", "desc", Status.OPEN, Priority.LOW, "eve", LocalDateTime.now(), LocalDateTime.now());
            List<Comment> comments = commentRepository.findByTicketOrderByCreatedAtAsc(ticket);
            assertThat(comments).isEmpty();
        }

        @Test
        @DisplayName("does not return comments belonging to another ticket")
        void doesNotReturnCommentsFromOtherTickets() {
            LocalDateTime now = LocalDateTime.now();

            Ticket ticketA = persistTicket("A", "descA", Status.OPEN, Priority.HIGH, "a", now, now);
            Ticket ticketB = persistTicket("B", "descB", Status.OPEN, Priority.LOW, "b", now, now);

            Comment c1 = persistComment(ticketB, "OnlyForB");
            pauseMillis(15);
            Comment c2 = persistComment(ticketA, "BelongsToA");
            pauseMillis(15);
            Comment c3 = persistComment(ticketA, "AlsoA");

            List<Comment> forA = commentRepository.findByTicketOrderByCreatedAtAsc(ticketA);

            assertThat(forA).hasSize(2);
            // Insert order: "BelongsToA" then "AlsoA"
            assertThat(forA).extracting(Comment::getBody).containsExactly("BelongsToA", "AlsoA");
            assertThat(forA).allMatch(comment -> comment.getTicket().getId().equals(ticketA.getId()));

            List<Comment> forB = commentRepository.findByTicketOrderByCreatedAtAsc(ticketB);
            assertThat(forB).hasSize(1);
            assertThat(forB.get(0).getBody()).isEqualTo("OnlyForB");
            assertThat(forB.get(0).getTicket().getId()).isEqualTo(ticketB.getId());
        }
    }
}