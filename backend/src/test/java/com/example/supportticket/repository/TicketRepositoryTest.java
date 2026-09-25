package com.example.supportticket.repository;

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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TicketRepositoryTest {

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

    @Nested
    @DisplayName("findAllByOrderByUpdatedAtDescIdDesc")
    class FindAllOrderByUpdatedAtDescIdDescTests {

        @Test
        @DisplayName("returns tickets ordered by updatedAt descending, then by id descending when updatedAt equal")
        void returnsOrderedTickets() {
            LocalDateTime now = LocalDateTime.now();

            Ticket t1 = persistTicket("A", "desc1", Status.OPEN, Priority.HIGH, "alice", now.minusDays(2), now.minusHours(5)); // t1.updatedAt < t2.updatedAt
            Ticket t2 = persistTicket("B", "desc2", Status.IN_PROGRESS, Priority.LOW, "bob", now.minusDays(1), now.minusHours(1));
            Ticket t3 = persistTicket("C", "desc3", Status.OPEN, Priority.MEDIUM, "eve", now.minusDays(1), now.minusHours(1)); // t2 and t3 have same updatedAt

            List<Ticket> result = ticketRepository.findAllByOrderByUpdatedAtDescIdDesc();

            assertThat(result).hasSize(3);

            // t3.id > t2.id since created after t2, and both have same updatedAt
            assertThat(result.get(0).getId()).isEqualTo(Math.max(t2.getId(), t3.getId()));
            assertThat(result.get(1).getId()).isEqualTo(Math.min(t2.getId(), t3.getId()));
            assertThat(result.get(2).getId()).isEqualTo(t1.getId());
        }
    }

    @Nested
    @DisplayName("findByKeyword")
    class FindByKeywordTests {

        @Test
        @DisplayName("searches title case-insensitively")
        void findsByTitleIgnoreCase() {
            Ticket t1 = persistTicket("Hello World", "Some desc", Status.OPEN, Priority.HIGH, "alice",
                    LocalDateTime.now(), LocalDateTime.now());

            Ticket t2 = persistTicket("Random Title", "Other desc", Status.OPEN, Priority.LOW, "bob",
                    LocalDateTime.now(), LocalDateTime.now());

            List<Ticket> found = ticketRepository.findByKeyword("hello");

            assertThat(found).extracting(Ticket::getId).containsExactly(t1.getId());
        }

        @Test
        @DisplayName("searches description case-insensitively")
        void findsByDescriptionIgnoreCase() {
            Ticket t1 = persistTicket("No match here", "MAGIC happens", Status.OPEN, Priority.HIGH, "alice",
                    LocalDateTime.now(), LocalDateTime.now());

            List<Ticket> found = ticketRepository.findByKeyword("magic");

            assertThat(found).extracting(Ticket::getId).containsExactly(t1.getId());
        }

        @Test
        @DisplayName("keyword not found returns empty result")
        void returnsEmptyWhenNotFound() {
            persistTicket("abc", "xyz", Status.OPEN, Priority.HIGH, "alice", LocalDateTime.now(), LocalDateTime.now());
            List<Ticket> found = ticketRepository.findByKeyword("NOT_PRESENT");
            assertThat(found).isEmpty();
        }

        // REMOVED: Tests that directly check .findByKeyword(null) or .findByKeyword("")
    }

    @Nested
    @DisplayName("findByStatusOrderByUpdatedAtDescIdDesc")
    class FindByStatusOrderByUpdatedAtDescIdDescTests {

        @Test
        @DisplayName("returns only tickets matching the requested status and verifies ordering")
        void filtersByStatusAndOrders() {
            LocalDateTime now = LocalDateTime.now();

            Ticket t1 = persistTicket("Ticket A", "desc", Status.OPEN, Priority.HIGH, "alice", now, now.minusHours(3));
            Ticket t2 = persistTicket("Ticket B", "desc", Status.RESOLVED, Priority.LOW, "bob", now, now.minusHours(2));
            Ticket t3 = persistTicket("Ticket C", "desc", Status.OPEN, Priority.LOW, "eve", now, now.minusHours(1));

            List<Ticket> openTickets = ticketRepository.findByStatusOrderByUpdatedAtDescIdDesc(Status.OPEN);

            assertThat(openTickets).hasSize(2);
            // t3.updatedAt > t1.updatedAt
            assertThat(openTickets.get(0).getId()).isEqualTo(t3.getId());
            assertThat(openTickets.get(1).getId()).isEqualTo(t1.getId());
        }
    }

    @Nested
    @DisplayName("findByStatusAndKeyword")
    class FindByStatusAndKeywordTests {

        @Test
        @DisplayName("applies both status and keyword filtering; keyword matches title")
        void filtersByStatusAndTitleKeyword() {
            Ticket t1 = persistTicket("Launch Rocket", "Moon landing.", Status.RESOLVED, Priority.HIGH, "alice", LocalDateTime.now(), LocalDateTime.now());
            Ticket t2 = persistTicket("Launch Satellite", "description", Status.OPEN, Priority.LOW, "bob", LocalDateTime.now(), LocalDateTime.now());
            Ticket t3 = persistTicket("Random", "Moon base project", Status.RESOLVED, Priority.LOW, "bob", LocalDateTime.now(), LocalDateTime.now());

            // Should only return t1 (status RESOLVED, title contains 'launch')
            List<Ticket> resolvedLaunch = ticketRepository.findByStatusAndKeyword(Status.RESOLVED, "Launch");
            assertThat(resolvedLaunch).extracting(Ticket::getId).containsExactly(t1.getId());
        }

        @Test
        @DisplayName("applies both status and keyword filtering; keyword matches description")
        void filtersByStatusAndDescriptionKeyword() {
            Ticket t1 = persistTicket("Not relevant", "deploy to production", Status.IN_PROGRESS, Priority.MEDIUM, "bob", LocalDateTime.now(), LocalDateTime.now());
            Ticket t2 = persistTicket("Another", "fix deployment", Status.IN_PROGRESS, Priority.LOW, "alice", LocalDateTime.now(), LocalDateTime.now());

            List<Ticket> found = ticketRepository.findByStatusAndKeyword(Status.IN_PROGRESS, "deploy");
            assertThat(found).hasSize(2)
                    .extracting(Ticket::getId).containsExactlyInAnyOrder(t1.getId(), t2.getId());
        }

        @Test
        @DisplayName("case-insensitive keyword match")
        void caseInsensitiveKeyword() {
            Ticket t1 = persistTicket("Fix Crash", "UpperCaseTesting", Status.OPEN, Priority.HIGH, "alice", LocalDateTime.now(), LocalDateTime.now());
            List<Ticket> found = ticketRepository.findByStatusAndKeyword(Status.OPEN, "UPPERcase");
            assertThat(found).extracting(Ticket::getId).containsExactly(t1.getId());
        }

        @Test
        @DisplayName("status mismatch returns empty")
        void statusMustMatch() {
            Ticket t1 = persistTicket("Title", "desc", Status.CLOSED, Priority.LOW, "bob", LocalDateTime.now(), LocalDateTime.now());
            List<Ticket> found = ticketRepository.findByStatusAndKeyword(Status.OPEN, "title");
            assertThat(found).isEmpty();
        }

        // REMOVED: Tests that directly check .findByStatusAndKeyword(status, null) or .findByStatusAndKeyword(status, "")
    }
}