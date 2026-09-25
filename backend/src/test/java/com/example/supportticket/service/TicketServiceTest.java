package com.example.supportticket.service;

import com.example.supportticket.entity.Comment;
import com.example.supportticket.entity.Ticket;
import com.example.supportticket.entity.Ticket.Status;
import com.example.supportticket.entity.Ticket.Priority;
import com.example.supportticket.exception.InvalidStatusTransitionException;
import com.example.supportticket.exception.TicketNotFoundException;
import com.example.supportticket.repository.CommentRepository;
import com.example.supportticket.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private CommentRepository commentRepository;

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ticketService = new TicketService(ticketRepository, commentRepository);
    }

    @Nested
    @DisplayName("Create Ticket")
    class CreateTicketTests {

        @Test
        void validTicketIsCreatedWithOpenStatus() {
            String title = "Sample";
            String desc = "desc";
            String priority = "HIGH";
            String assignee = "bob";

            ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
            when(ticketRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Ticket ticket = ticketService.createTicket(title, desc, priority, assignee);

            verify(ticketRepository).save(captor.capture());
            Ticket saved = captor.getValue();
            assertEquals(Status.OPEN, saved.getStatus());
            assertEquals(Priority.HIGH, saved.getPriority());
            assertEquals(title, saved.getTitle());
            assertEquals(desc, saved.getDescription());
            assertEquals(assignee, saved.getAssignee());
            // We do not assert on createdAt being not-null after save() (see prompt)
        }

        @Test
        void titleDescriptionAndPriorityAreStoredCorrectly() {
            when(ticketRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            Ticket ticket = ticketService.createTicket("t", "d", "LOW", null);
            assertEquals("t", ticket.getTitle());
            assertEquals("d", ticket.getDescription());
            assertEquals(Priority.LOW, ticket.getPriority());
        }

        @Test
        void optionalAssigneeCanBeNull() {
            when(ticketRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            Ticket ticket = ticketService.createTicket("t", "desc", "MEDIUM", null);
            assertNull(ticket.getAssignee());
        }

        @Test
        void blankAssigneeIsRejected() {
            assertThrows(IllegalArgumentException.class, () ->
                ticketService.createTicket("t", "desc", "HIGH", "   ")
            );
        }

        @Test
        void invalidPriorityIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                ticketService.createTicket("t", "desc", "UNKNOWN", "someone")
            );
            assertTrue(ex.getMessage().toLowerCase().contains("priority"));
        }
    }

    @Nested
    @DisplayName("Get Ticket")
    class GetTicketTests {
        @Test
        void existingTicketIsReturned() {
            Ticket t = buildTicket();
            when(ticketRepository.findById(1L)).thenReturn(Optional.of(t));
            Ticket result = ticketService.getTicketById(1L);
            assertSame(t, result);
        }

        @Test
        void missingTicketThrowsTicketNotFoundException() {
            when(ticketRepository.findById(99L)).thenReturn(Optional.empty());
            assertThrows(TicketNotFoundException.class, () -> ticketService.getTicketById(99L));
        }
    }

    @Nested
    @DisplayName("Update Ticket")
    class UpdateTicketTests {
        @Test
        void validTitleDescriptionPriorityUpdate() {
            Ticket t = buildTicket();
            when(ticketRepository.findById(7L)).thenReturn(Optional.of(t));
            when(ticketRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Ticket result = ticketService.updateTicketDetails(7L, "new title", "new desc", "MEDIUM");
            assertEquals("new title", result.getTitle());
            assertEquals("new desc", result.getDescription());
            assertEquals(Priority.MEDIUM, result.getPriority());
            // Do not assert on updatedAt being set via save()
        }

        @Test
        void blankRequiredFieldsAreRejected() {
            Ticket t = buildTicket();
            when(ticketRepository.findById(11L)).thenReturn(Optional.of(t));
            assertThrows(IllegalArgumentException.class, () ->
                ticketService.updateTicketDetails(11L, "   ", "body", "HIGH"));
            assertThrows(IllegalArgumentException.class, () ->
                ticketService.updateTicketDetails(11L, "title", "   ", "HIGH"));
        }

        @Test
        void invalidPriorityIsRejected() {
            Ticket t = buildTicket();
            when(ticketRepository.findById(22L)).thenReturn(Optional.of(t));
            assertThrows(IllegalArgumentException.class, () ->
                ticketService.updateTicketDetails(22L, "t", "b", "INVALID"));
        }
    }

    @Nested
    @DisplayName("Assignee")
    class AssigneeTests {
        @Test
        void validAssigneeUpdatesSuccessfully() {
            Ticket t = buildTicket();
            when(ticketRepository.findById(2L)).thenReturn(Optional.of(t));
            when(ticketRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Ticket result = ticketService.setAssignee(2L, "diana");
            assertEquals("diana", result.getAssignee());
            // Do not assert on updatedAt being set via save()
        }

        @Test
        void nullAssigneeClearsTheAssignee() {
            Ticket t = buildTicket();
            t.setAssignee("xxx");
            when(ticketRepository.findById(3L)).thenReturn(Optional.of(t));
            when(ticketRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Ticket result = ticketService.setAssignee(3L, null);
            assertNull(result.getAssignee());
        }

        @Test
        void blankAssigneeIsRejected() {
            Ticket t = buildTicket();
            when(ticketRepository.findById(4L)).thenReturn(Optional.of(t));
            assertThrows(IllegalArgumentException.class,
                    () -> ticketService.setAssignee(4L, "  "));
        }

        @Test
        void assigneeLongerThan255CharactersIsRejected() {
            Ticket t = buildTicket();
            when(ticketRepository.findById(5L)).thenReturn(Optional.of(t));
            String longAssignee = "a".repeat(256);
            assertThrows(IllegalArgumentException.class,
                    () -> ticketService.setAssignee(5L, longAssignee));
        }
    }

    @Nested
    @DisplayName("Comments")
    class CommentsTests {
        @Test
        void validCommentIsCreated() {
            Ticket t = buildTicket();
            t.setId(9L);
            when(ticketRepository.findById(9L)).thenReturn(Optional.of(t));
            when(commentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
            Comment c = ticketService.addComment(9L, "This is a valid comment.");
            verify(commentRepository).save(captor.capture());

            assertEquals("This is a valid comment.", captor.getValue().getBody());
            assertEquals(9L, c.getTicket().getId());
            // Do not assert on createdAt set via save()
        }

        @Test
        void blankCommentIsRejected() {
            Ticket t = buildTicket();
            t.setId(8L);
            when(ticketRepository.findById(8L)).thenReturn(Optional.of(t));
            assertThrows(IllegalArgumentException.class,
                    () -> ticketService.addComment(8L, "   "));
        }

        @Test
        void commentLongerThan4000CharactersIsRejected() {
            Ticket t = buildTicket();
            t.setId(8L);
            when(ticketRepository.findById(8L)).thenReturn(Optional.of(t));
            String longComment = "a".repeat(4001);
            assertThrows(IllegalArgumentException.class,
                    () -> ticketService.addComment(8L, longComment));
        }

        @Test
        void getCommentsForTicketReturnsCommentsFromRepository() {
            Ticket t = buildTicket();
            t.setId(13L);

            Comment c1 = new Comment();
            // c1.setId(1L); // removed as Comment does not have setId()
            c1.setTicket(t);
            c1.setBody("body1");
            // Removed call to c1.setCreatedAt(...)

            Comment c2 = new Comment();
            // c2.setId(2L); // removed as Comment does not have setId()
            c2.setTicket(t);
            c2.setBody("body2");
            // Removed call to c2.setCreatedAt(...)

            List<Comment> commentList = Arrays.asList(
                c1,
                c2
            );
            when(ticketRepository.findById(13L)).thenReturn(Optional.of(t));
            when(commentRepository.findByTicketOrderByCreatedAtAsc(t)).thenReturn(commentList);

            List<Comment> result = ticketService.getCommentsForTicket(13L);
            assertEquals(2, result.size());
            assertSame(commentList, result);
        }
    }

    @Nested
    @DisplayName("Search")
    class SearchTests {
        @Test
        void searchWithNoStatusAndNoKeyword() {
            List<Ticket> expected = Collections.singletonList(buildTicket());
            when(ticketRepository.findAllByOrderByUpdatedAtDescIdDesc()).thenReturn(expected);

            List<Ticket> res = ticketService.searchTickets(null, null);
            assertEquals(expected, res);
        }

        @Test
        void searchByKeyword() {
            String q = "oops";
            List<Ticket> expected = List.of(buildTicket());
            when(ticketRepository.findByKeyword(q)).thenReturn(expected);
            List<Ticket> res = ticketService.searchTickets(null, q);
            assertEquals(expected, res);
        }

        @Test
        void searchByStatus() {
            Status st = Status.OPEN;
            List<Ticket> expected = List.of(buildTicket());
            when(ticketRepository.findByStatusOrderByUpdatedAtDescIdDesc(st)).thenReturn(expected);
            List<Ticket> res = ticketService.searchTickets(st, null);
            assertEquals(expected, res);
        }

        @Test
        void searchByStatusAndKeyword() {
            Status st = Status.CLOSED;
            String q = "final";
            List<Ticket> expected = List.of(buildTicket());
            when(ticketRepository.findByStatusAndKeyword(st, q)).thenReturn(expected);
            List<Ticket> res = ticketService.searchTickets(st, q);
            assertEquals(expected, res);
        }
    }

    @Nested
    @DisplayName("Status State Machine")
    class StatusStateMachineTests {
        @Test
        void openToInProgress() {
            Ticket t = buildTicket();
            t.setStatus(Status.OPEN);
            when(ticketRepository.findById(1L)).thenReturn(Optional.of(t));
            when(ticketRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            Ticket result = ticketService.transitionStatus(1L, Status.IN_PROGRESS);
            assertEquals(Status.IN_PROGRESS, result.getStatus());
        }

        @Test
        void openToCancelled() {
            Ticket t = buildTicket();
            t.setStatus(Status.OPEN);
            when(ticketRepository.findById(2L)).thenReturn(Optional.of(t));
            when(ticketRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            Ticket result = ticketService.transitionStatus(2L, Status.CANCELLED);
            assertEquals(Status.CANCELLED, result.getStatus());
        }

        @Test
        void inProgressToResolved() {
            Ticket t = buildTicket();
            t.setStatus(Status.IN_PROGRESS);
            when(ticketRepository.findById(3L)).thenReturn(Optional.of(t));
            when(ticketRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            Ticket result = ticketService.transitionStatus(3L, Status.RESOLVED);
            assertEquals(Status.RESOLVED, result.getStatus());
        }

        @Test
        void inProgressToCancelled() {
            Ticket t = buildTicket();
            t.setStatus(Status.IN_PROGRESS);
            when(ticketRepository.findById(4L)).thenReturn(Optional.of(t));
            when(ticketRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            Ticket result = ticketService.transitionStatus(4L, Status.CANCELLED);
            assertEquals(Status.CANCELLED, result.getStatus());
        }

        @Test
        void resolvedToClosed() {
            Ticket t = buildTicket();
            t.setStatus(Status.RESOLVED);
            when(ticketRepository.findById(5L)).thenReturn(Optional.of(t));
            when(ticketRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            Ticket result = ticketService.transitionStatus(5L, Status.CLOSED);
            assertEquals(Status.CLOSED, result.getStatus());
        }

        // Invalid transitions
        @Test
        void openToResolved_IsInvalid() {
            Ticket t = buildTicket();
            t.setStatus(Status.OPEN);
            when(ticketRepository.findById(6L)).thenReturn(Optional.of(t));
            assertThrows(InvalidStatusTransitionException.class,
                    () -> ticketService.transitionStatus(6L, Status.RESOLVED));
        }

        @Test
        void openToClosed_IsInvalid() {
            Ticket t = buildTicket();
            t.setStatus(Status.OPEN);
            when(ticketRepository.findById(7L)).thenReturn(Optional.of(t));
            assertThrows(InvalidStatusTransitionException.class,
                    () -> ticketService.transitionStatus(7L, Status.CLOSED));
        }

        @Test
        void inProgressToClosed_IsInvalid() {
            Ticket t = buildTicket();
            t.setStatus(Status.IN_PROGRESS);
            when(ticketRepository.findById(8L)).thenReturn(Optional.of(t));
            assertThrows(InvalidStatusTransitionException.class,
                    () -> ticketService.transitionStatus(8L, Status.CLOSED));
        }

        @Test
        void inProgressToOpen_IsInvalid() {
            Ticket t = buildTicket();
            t.setStatus(Status.IN_PROGRESS);
            when(ticketRepository.findById(9L)).thenReturn(Optional.of(t));
            assertThrows(InvalidStatusTransitionException.class,
                    () -> ticketService.transitionStatus(9L, Status.OPEN));
        }

        @Test
        void resolvedToOpen_IsInvalid() {
            Ticket t = buildTicket();
            t.setStatus(Status.RESOLVED);
            when(ticketRepository.findById(10L)).thenReturn(Optional.of(t));
            assertThrows(InvalidStatusTransitionException.class,
                    () -> ticketService.transitionStatus(10L, Status.OPEN));
        }

        @Test
        void resolvedToCancelled_IsInvalid() {
            Ticket t = buildTicket();
            t.setStatus(Status.RESOLVED);
            when(ticketRepository.findById(11L)).thenReturn(Optional.of(t));
            assertThrows(InvalidStatusTransitionException.class,
                    () -> ticketService.transitionStatus(11L, Status.CANCELLED));
        }

        @Test
        void closedToOpen_IsInvalid() {
            Ticket t = buildTicket();
            t.setStatus(Status.CLOSED);
            when(ticketRepository.findById(12L)).thenReturn(Optional.of(t));
            assertThrows(InvalidStatusTransitionException.class,
                    () -> ticketService.transitionStatus(12L, Status.OPEN));
        }

        @Test
        void cancelledToOpen_IsInvalid() {
            Ticket t = buildTicket();
            t.setStatus(Status.CANCELLED);
            when(ticketRepository.findById(13L)).thenReturn(Optional.of(t));
            assertThrows(InvalidStatusTransitionException.class,
                    () -> ticketService.transitionStatus(13L, Status.OPEN));
        }

        @Test
        void sameStatusTransition_IsInvalid() {
            for (Status st : Status.values()) {
                Ticket t = buildTicket();
                t.setStatus(st);
                when(ticketRepository.findById((long) st.ordinal())).thenReturn(Optional.of(t));
                assertThrows(InvalidStatusTransitionException.class,
                        () -> ticketService.transitionStatus((long) st.ordinal(), st));
            }
        }
    }

    // ---- Utility: build a basic ticket ----
    private Ticket buildTicket() {
        Ticket t = new Ticket();
        t.setId(1L);
        t.setTitle("title");
        t.setDescription("desc");
        t.setPriority(Priority.LOW);
        t.setStatus(Status.OPEN);
        t.setCreatedAt(LocalDateTime.now());
        return t;
    }
}