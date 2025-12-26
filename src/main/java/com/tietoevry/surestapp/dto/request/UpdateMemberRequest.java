package com.tietoevry.surestapp.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record UpdateMemberRequest(
    @NotBlank(message = "First name is required")
    @JsonProperty("firstName")
    String firstName,

    @NotBlank(message = "Last name is required")
    @JsonProperty("lastName")
    String lastName,

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @JsonProperty("dateOfBirth")
    LocalDate dateOfBirth,

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @JsonProperty("email")
    String email
) {}
