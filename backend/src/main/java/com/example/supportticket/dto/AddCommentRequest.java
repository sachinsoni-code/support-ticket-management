package com.example.supportticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AddCommentRequest {

    @NotBlank
    @Size(max = 4000)
    private String body;

    public AddCommentRequest() {
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}