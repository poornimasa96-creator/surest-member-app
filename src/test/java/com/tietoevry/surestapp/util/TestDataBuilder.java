package com.tietoevry.surestapp.util;

import com.tietoevry.surestapp.dto.request.CreateMemberRequest;
import com.tietoevry.surestapp.dto.request.LoginRequest;
import com.tietoevry.surestapp.dto.request.UpdateMemberRequest;
import com.tietoevry.surestapp.dto.response.LoginResponse;
import com.tietoevry.surestapp.dto.response.MemberResponse;
import com.tietoevry.surestapp.dto.response.PagedMemberResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class TestDataBuilder {

    // LoginRequest builders
    public static LoginRequest validLoginRequest() {
        return new LoginRequest("admin", "password123");
    }

    public static LoginRequest loginRequestWithUsername(String username) {
        return new LoginRequest(username, "password123");
    }

    public static LoginRequest loginRequestWithPassword(String password) {
        return new LoginRequest("admin", password);
    }

    public static LoginRequest loginRequestWith(String username, String password) {
        return new LoginRequest(username, password);
    }

    // LoginResponse builders
    public static LoginResponse adminLoginResponse() {
        return new LoginResponse(
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJBRE1JTiJ9",
            "admin",
            "ADMIN"
        );
    }

    public static LoginResponse userLoginResponse() {
        return new LoginResponse(
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyIiwicm9sZSI6IlVTRVIifQ",
            "user",
            "USER"
        );
    }

    public static LoginResponse loginResponseWith(String username, String role) {
        return new LoginResponse(
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.token_for_" + username,
            username,
            role
        );
    }

    // MemberResponse builders
    public static MemberResponse memberResponse() {
        return new MemberResponse(
            UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
            "John",
            "Doe",
            LocalDate.of(1990, 1, 1),
            "john.doe@example.com",
            LocalDateTime.of(2024, 1, 1, 10, 0),
            LocalDateTime.of(2024, 1, 1, 10, 0)
        );
    }

    public static MemberResponse memberResponseWithId(UUID id) {
        return new MemberResponse(
            id,
            "John",
            "Doe",
            LocalDate.of(1990, 1, 1),
            "john.doe@example.com",
            LocalDateTime.of(2024, 1, 1, 10, 0),
            LocalDateTime.of(2024, 1, 1, 10, 0)
        );
    }

    public static MemberResponse memberResponseWith(String firstName, String lastName, String email) {
        return new MemberResponse(
            UUID.randomUUID(),
            firstName,
            lastName,
            LocalDate.of(1990, 1, 1),
            email,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }

    // CreateMemberRequest builders
    public static CreateMemberRequest validCreateMemberRequest() {
        return new CreateMemberRequest(
            "John",
            "Doe",
            LocalDate.of(1990, 1, 1),
            "john.doe@example.com"
        );
    }

    public static CreateMemberRequest createMemberRequestWithEmail(String email) {
        return new CreateMemberRequest(
            "John",
            "Doe",
            LocalDate.of(1990, 1, 1),
            email
        );
    }

    public static CreateMemberRequest createMemberRequestWith(String firstName, String lastName, LocalDate dob, String email) {
        return new CreateMemberRequest(firstName, lastName, dob, email);
    }

    // UpdateMemberRequest builders
    public static UpdateMemberRequest validUpdateMemberRequest() {
        return new UpdateMemberRequest(
            "John",
            "Doe",
            LocalDate.of(1990, 1, 1),
            "john.doe@example.com"
        );
    }

    public static UpdateMemberRequest updateMemberRequestWithEmail(String email) {
        return new UpdateMemberRequest(
            "John",
            "Doe",
            LocalDate.of(1990, 1, 1),
            email
        );
    }

    public static UpdateMemberRequest updateMemberRequestWith(String firstName, String lastName, LocalDate dob, String email) {
        return new UpdateMemberRequest(firstName, lastName, dob, email);
    }

    // PagedMemberResponse builders
    public static PagedMemberResponse pagedMemberResponse(List<MemberResponse> members, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean isLast = page >= totalPages - 1;
        return new PagedMemberResponse(
            members,
            page,
            size,
            totalElements,
            totalPages,
            isLast
        );
    }

    public static PagedMemberResponse emptyPagedResponse() {
        return new PagedMemberResponse(
            List.of(),
            0,
            20,
            0,
            0,
            true
        );
    }

    public static PagedMemberResponse singlePageResponse(List<MemberResponse> members) {
        return pagedMemberResponse(members, 0, 20, members.size());
    }
}
