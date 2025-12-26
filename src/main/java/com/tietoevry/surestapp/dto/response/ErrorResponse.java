package com.tietoevry.surestapp.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    @JsonProperty("timestamp")
    LocalDateTime timestamp,

    @JsonProperty("status")
    int status,

    @JsonProperty("error")
    String error,

    @JsonProperty("message")
    String message,

    @JsonProperty("path")
    String path,

    @JsonProperty("errors")
    List<String> errors
) {
    public ErrorResponse(int status, String error, String message, String path) {
        this(LocalDateTime.now(), status, error, message, path, null);
    }

    public ErrorResponse(int status, String error, String message, String path, List<String> errors) {
        this(LocalDateTime.now(), status, error, message, path, errors);
    }
}
