package com.tietoevry.surestapp.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MemberResponse(
    @JsonProperty("id")
    UUID id,

    @JsonProperty("firstName")
    String firstName,

    @JsonProperty("lastName")
    String lastName,

    @JsonProperty("dateOfBirth")
    LocalDate dateOfBirth,

    @JsonProperty("email")
    String email,

    @JsonProperty("createdAt")
    LocalDateTime createdAt,

    @JsonProperty("updatedAt")
    LocalDateTime updatedAt
) {}
