package com.example.supportticket.repository;

import com.example.supportticket.entity.Comment;
import com.example.supportticket.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * Retrieve all comments for a specific ticket, ordered by creation time ascending (oldest first).
     *
     * @param ticket The Ticket entity for which to retrieve comments.
     * @return List of Comment entities, oldest first.
     */
    List<Comment> findByTicketOrderByCreatedAtAsc(Ticket ticket);
}