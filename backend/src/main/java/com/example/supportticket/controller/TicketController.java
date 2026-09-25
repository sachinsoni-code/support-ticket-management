package com.example.supportticket.controller;

import com.example.supportticket.dto.*;
import com.example.supportticket.entity.Ticket;
import com.example.supportticket.entity.Comment;
import com.example.supportticket.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketService ticketService;

    @Autowired
    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    // POST /api/v1/tickets
    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        Ticket ticket = ticketService.createTicket(
                request.getTitle(),
                request.getDescription(),
                request.getPriority(),
                request.getAssignee()
        );
        List<CommentResponse> comments = ticketService.getCommentsForTicket(ticket.getId())
                .stream()
                .map(this::mapToCommentResponse)
                .collect(Collectors.toList());
        TicketResponse ticketResponse = mapToTicketResponse(ticket, comments);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{ticketId}")
                .buildAndExpand(ticketResponse.getTicketId())
                .toUri();

        return ResponseEntity.created(location).body(ticketResponse);
    }

    // GET /api/v1/tickets?q=...&status=...
    @GetMapping
    public ResponseEntity<List<TicketResponse>> getTickets(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "status", required = false) String statusString
    ) {
        Ticket.Status status = null;
        if (statusString != null) {
            try {
                status = Ticket.Status.valueOf(statusString.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw ex;
            }
        }
        List<Ticket> tickets = ticketService.searchTickets(status, q);
        List<TicketResponse> responses = tickets.stream()
                .map(ticket -> mapToTicketResponse(ticket, Collections.emptyList()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    // GET /api/v1/tickets/{ticketId}
    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable Long ticketId) {
        Ticket ticket = ticketService.getTicketById(ticketId);
        List<CommentResponse> comments = ticketService.getCommentsForTicket(ticketId)
                .stream()
                .map(this::mapToCommentResponse)
                .collect(Collectors.toList());
        TicketResponse response = mapToTicketResponse(ticket, comments);
        return ResponseEntity.ok(response);
    }

    // PATCH /api/v1/tickets/{ticketId}
    @PatchMapping("/{ticketId}")
    public ResponseEntity<TicketResponse> updateTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody UpdateTicketRequest request
    ) {
        Ticket ticket = ticketService.updateTicketDetails(
                ticketId,
                request.getTitle(),
                request.getDescription(),
                request.getPriority()
        );
        TicketResponse response = mapToTicketResponse(ticket, Collections.emptyList());
        return ResponseEntity.ok(response);
    }

    // PATCH /api/v1/tickets/{ticketId}/status
    @PatchMapping("/{ticketId}/status")
    public ResponseEntity<TicketResponse> updateStatus(
            @PathVariable Long ticketId,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        Ticket.Status status;
        try {
            status = Ticket.Status.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw ex;
        }
        Ticket ticket = ticketService.transitionStatus(ticketId, status);
        TicketResponse response = mapToTicketResponse(ticket, Collections.emptyList());
        return ResponseEntity.ok(response);
    }

    // PATCH /api/v1/tickets/{ticketId}/assignee
    @PatchMapping("/{ticketId}/assignee")
    public ResponseEntity<TicketResponse> setAssignee(
            @PathVariable Long ticketId,
            @Valid @RequestBody AssigneeRequest request
    ) {
        String assignee = request.getAssignee();
        if (assignee != null && assignee.trim().isEmpty()) {
            throw new IllegalArgumentException("Assignee must not be blank.");
        }
        Ticket ticket = ticketService.setAssignee(ticketId, assignee);
        TicketResponse response = mapToTicketResponse(ticket, Collections.emptyList());
        return ResponseEntity.ok(response);
    }

    // POST /api/v1/tickets/{ticketId}/comments
    @PostMapping("/{ticketId}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long ticketId,
            @Valid @RequestBody AddCommentRequest request
    ) {
        Comment comment = ticketService.addComment(ticketId, request.getBody());
        CommentResponse response = mapToCommentResponse(comment);

        // Do not attempt to produce a GET location for a comment.
        return ResponseEntity.status(201).body(response);
    }

    // ----- Mapping utility methods -----

    private TicketResponse mapToTicketResponse(Ticket ticket, List<CommentResponse> comments) {
        TicketResponse dto = new TicketResponse();
        dto.setTicketId(ticket.getId());
        dto.setTitle(ticket.getTitle());
        dto.setDescription(ticket.getDescription());
        dto.setStatus(ticket.getStatus() != null ? ticket.getStatus().name() : null);
        dto.setPriority(ticket.getPriority() != null ? ticket.getPriority().name() : null);
        dto.setAssignee(ticket.getAssignee());
        if (ticket.getCreatedAt() != null) {
            dto.setCreatedAt(ticket.getCreatedAt().atOffset(ZoneOffset.UTC));
        }
        if (ticket.getUpdatedAt() != null) {
            dto.setUpdatedAt(ticket.getUpdatedAt().atOffset(ZoneOffset.UTC));
        }
        dto.setComments(comments);
        return dto;
    }

    private CommentResponse mapToCommentResponse(Comment comment) {
        CommentResponse dto = new CommentResponse();
        dto.setCommentId(comment.getId());
        dto.setBody(comment.getBody());
        if (comment.getCreatedAt() != null) {
            dto.setCreatedAt(comment.getCreatedAt().atOffset(ZoneOffset.UTC));
        }
        return dto;
    }
}