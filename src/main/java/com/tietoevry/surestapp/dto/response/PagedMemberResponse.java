package com.tietoevry.surestapp.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PagedMemberResponse(
    @JsonProperty("content")
    List<MemberResponse> content,

    @JsonProperty("page")
    int page,

    @JsonProperty("size")
    int size,

    @JsonProperty("totalElements")
    long totalElements,

    @JsonProperty("totalPages")
    int totalPages,

    @JsonProperty("last")
    boolean last
) {}
