package com.example.supportticket.service;

import com.example.supportticket.entity.Ticket;
import com.example.supportticket.entity.Comment;
import com.example.supportticket.entity.Ticket.Status;
import com.example.supportticket.entity.Ticket.Priority;
import com.example.supportticket.repository.TicketRepository;
import com.example.supportticket.repository.CommentRepository;
import com.example.supportticket.exception.TicketNotFoundException;
import com.example.supportticket.exception.InvalidStatusTransitionException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;

    // Map of allowed transitions for each status
    private static final Map<Status, Set<Status>> ALLOWED_TRANSITIONS;
    static {
        Map<Status, Set<Status>> map = new EnumMap<>(Status.class);
        map.put(Status.OPEN, Set.of(Status.IN_PROGRESS, Status.CANCELLED));
        map.put(Status.IN_PROGRESS, Set.of(Status.RESOLVED, Status.CANCELLED));
        map.put(Status.RESOLVED, Set.of(Status.CLOSED));
        map.put(Status.CLOSED, Collections.emptySet());
        map.put(Status.CANCELLED, Collections.emptySet());
        ALLOWED_TRANSITIONS = Collections.unmodifiableMap(map);
    }

    public TicketService(TicketRepository ticketRepository, CommentRepository commentRepository) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
    }

    /**
     * Create a new support ticket.
     *
     * @param title required, not blank
     * @param description required, not blank
     * @param priority required, not blank or null (LOW, MEDIUM, HIGH)
     * @param assignee optional, following contract (see method)
     * @return the created Ticket entity
     */
    @Transactional
    public Ticket createTicket(String title, String description, String priority, String assignee) {
        if (isBlank(title)) throw new IllegalArgumentException("Title is required");
        if (isBlank(description)) throw new IllegalArgumentException("Description is required");
        if (isBlank(priority)) throw new IllegalArgumentException("Priority is required");

        Priority parsedPriority = parseAndValidatePriority(priority);

        Ticket ticket = new Ticket();
        ticket.setTitle(title.trim());
        ticket.setDescription(description.trim());
        ticket.setPriority(parsedPriority);
        ticket.setStatus(Status.OPEN);

        if (assignee == null) {
            ticket.setAssignee(null);
        } else {
            String trimmed = assignee.trim();
            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException("Assignee cannot be blank");
            }
            if (trimmed.length() > 255) {
                throw new IllegalArgumentException("Assignee max length is 255 characters");
            }
            ticket.setAssignee(trimmed);
        }
        return ticketRepository.save(ticket);
    }

    /**
     * Find a ticket by its ID.
     *
     * @param id Ticket id
     * @return Ticket entity
     * @throws TicketNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public Ticket getTicketById(Long id) {
        return ticketRepository.findById(id)
            .orElseThrow(() -> new TicketNotFoundException("Ticket with id " + id + " not found"));
    }

    /**
     * List/search/filter tickets.
     *
     * @param status optional Ticket.Status; if null, don't filter by status
     * @param keyword optional search keyword for title/description, case-insensitive;
     *                blank or null means no keyword filter
     * @return tickets matching the filter, newest first by updatedAt then id
     */
    @Transactional(readOnly = true)
    public List<Ticket> searchTickets(Status status, String keyword) {
        String search = isBlank(keyword) ? null : keyword.trim();
        if (status == null && search == null) {
            return ticketRepository.findAllByOrderByUpdatedAtDescIdDesc();
        } else if (status == null) {
            return ticketRepository.findByKeyword(search);
        } else if (search == null) {
            return ticketRepository.findByStatusOrderByUpdatedAtDescIdDesc(status);
        } else {
            return ticketRepository.findByStatusAndKeyword(status, search);
        }
    }

    /**
     * Update title, description, and priority together (no partial update).
     *
     * @param ticketId the ID of the ticket
     * @param title required, not blank
     * @param description required, not blank
     * @param priority required, not blank (LOW, MEDIUM, HIGH)
     * @return updated Ticket entity
     * @throws TicketNotFoundException if not found
     */
    @Transactional
    public Ticket updateTicketDetails(Long ticketId, String title, String description, String priority) {
        Ticket ticket = getTicketById(ticketId);

        if (isBlank(title)) throw new IllegalArgumentException("Title is required");
        if (isBlank(description)) throw new IllegalArgumentException("Description is required");
        if (isBlank(priority)) throw new IllegalArgumentException("Priority is required");

        Priority parsedPriority = parseAndValidatePriority(priority);

        ticket.setTitle(title.trim());
        ticket.setDescription(description.trim());
        ticket.setPriority(parsedPriority);
        return ticketRepository.save(ticket);
    }

    /**
     * Set or clear the assignee for a ticket.
     * null = clear; blank or whitespace only = validation error;
     * non-blank = trimmed, max length 255
     *
     * @param ticketId the ID of the ticket
     * @param assignee new assignee username; see contract
     * @return updated Ticket entity
     * @throws TicketNotFoundException if not found
     * @throws IllegalArgumentException if blank/invalid provided for assignee set
     */
    @Transactional
    public Ticket setAssignee(Long ticketId, String assignee) {
        Ticket ticket = getTicketById(ticketId);

        if (assignee == null) {
            ticket.setAssignee(null);
        } else {
            String trimmed = assignee.trim();
            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException("Assignee cannot be blank");
            }
            if (trimmed.length() > 255) {
                throw new IllegalArgumentException("Assignee max length is 255 characters");
            }
            ticket.setAssignee(trimmed);
        }
        return ticketRepository.save(ticket);
    }

    /**
     * Add a comment to a ticket.
     *
     * @param ticketId the ticket's ID
     * @param body non-blank comment body, max 4000 chars
     * @return the created Comment entity
     * @throws TicketNotFoundException if ticket missing
     * @throws IllegalArgumentException if body invalid
     */
    @Transactional
    public Comment addComment(Long ticketId, String body) {
        Ticket ticket = getTicketById(ticketId);
        if (isBlank(body)) throw new IllegalArgumentException("Comment body must not be blank");
        String commentBody = body.trim();
        if (commentBody.length() > 4000) throw new IllegalArgumentException("Comment body too long (max 4000)");
        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setBody(commentBody);
        return commentRepository.save(comment);
    }

    /**
     * Get all comments for a ticket, oldest first.
     *
     * @param ticketId the ticket's ID
     * @return list of comments, oldest first
     * @throws TicketNotFoundException if the ticket does not exist
     */
    @Transactional(readOnly = true)
    public List<Comment> getCommentsForTicket(Long ticketId) {
        Ticket ticket = getTicketById(ticketId);
        return commentRepository.findByTicketOrderByCreatedAtAsc(ticket);
    }

    /**
     * Change ticket status, enforcing the state machine rules.
     *
     * @param ticketId Ticket ID
     * @param newStatus The desired new status
     * @return updated Ticket entity
     * @throws TicketNotFoundException if ticket not found
     * @throws InvalidStatusTransitionException if transition not allowed
     */
    @Transactional
    public Ticket transitionStatus(Long ticketId, Status newStatus) {
        Ticket ticket = getTicketById(ticketId);
        Status from = ticket.getStatus();

        if (from == newStatus) {
            throw new InvalidStatusTransitionException("Same-status transitions are not permitted: " + from);
        }

        Set<Status> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, Collections.emptySet());
        if (!allowed.contains(newStatus)) {
            throw new InvalidStatusTransitionException(
                    String.format("Cannot transition ticket %d from %s to %s", ticketId, from, newStatus)
            );
        }

        ticket.setStatus(newStatus);
        return ticketRepository.save(ticket);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static Priority parseAndValidatePriority(String priority) {
        String trimmed = priority == null ? null : priority.trim();
        if (trimmed == null || trimmed.isEmpty()) {
            throw new IllegalArgumentException("Priority is required");
        }
        try {
            Priority prio = Priority.valueOf(trimmed.toUpperCase());
            if (prio != Priority.LOW && prio != Priority.MEDIUM && prio != Priority.HIGH) {
                throw new IllegalArgumentException("Priority must be one of: LOW, MEDIUM, HIGH");
            }
            return prio;
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Priority must be one of: LOW, MEDIUM, HIGH");
        }
    }
}