package com.tietoevry.surestapp.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginResponse(
    @JsonProperty("token")
    String token,

    @JsonProperty("type")
    String type,

    @JsonProperty("username")
    String username,

    @JsonProperty("role")
    String role
) {
    public LoginResponse(String token, String username, String role) {
        this(token, "Bearer", username, role);
    }
}
