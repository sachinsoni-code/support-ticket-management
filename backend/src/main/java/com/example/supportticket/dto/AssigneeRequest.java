package com.example.supportticket.dto;

import jakarta.validation.constraints.Size;

public class AssigneeRequest {

    @Size(max = 255)
    private String assignee;

    public AssigneeRequest() {
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }
}