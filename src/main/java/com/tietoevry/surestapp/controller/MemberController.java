package com.tietoevry.surestapp.controller;

import com.tietoevry.surestapp.dto.request.CreateMemberRequest;
import com.tietoevry.surestapp.dto.request.UpdateMemberRequest;
import com.tietoevry.surestapp.dto.response.ErrorResponse;
import com.tietoevry.surestapp.dto.response.MemberResponse;
import com.tietoevry.surestapp.dto.response.PagedMemberResponse;
import com.tietoevry.surestapp.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(value = "api/v1/members", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Members", description = "Member management APIs with CRUD operations")
@SecurityRequirement(name = "Bearer Authentication")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
        summary = "Get all members",
        description = "Retrieve a paginated list of members with optional filtering by first name or last name. Supports sorting and pagination."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved members",
            content = @Content(schema = @Schema(implementation = PagedMemberResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - JWT token missing or invalid",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Insufficient permissions",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<PagedMemberResponse> getAllMembers(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Field to sort by", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "Sort direction (ASC or DESC)", example = "DESC")
            @RequestParam(defaultValue = "DESC") String direction,
            @Parameter(description = "Filter by first name (case-insensitive partial match)")
            @RequestParam(required = false) String firstName,
            @Parameter(description = "Filter by last name (case-insensitive partial match)")
            @RequestParam(required = false) String lastName) {

        log.info("Received request to get all members - page: {}, size: {}, sort: {}, direction: {}", page, size, sort, direction);

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        PagedMemberResponse response = memberService.getAllMembers(
            pageRequest, firstName, lastName);
        log.info("Successfully retrieved {} members", response.content().size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
        summary = "Get member by ID",
        description = "Retrieve a single member by their unique identifier. This endpoint uses caching for improved performance."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved member",
            content = @Content(schema = @Schema(implementation = MemberResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Member not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - JWT token missing or invalid",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<MemberResponse> getMemberById(
            @Parameter(description = "Member UUID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id) {
        log.info("Received request to get member with ID: {}", id);
        MemberResponse response = memberService.getMemberById(id);
        log.info("Successfully retrieved member with ID: {}", id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Create new member",
        description = "Create a new member. Only accessible by users with ADMIN role. Email must be unique."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Member successfully created",
            content = @Content(schema = @Schema(implementation = MemberResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data or duplicate email",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - JWT token missing or invalid",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - ADMIN role required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<MemberResponse> createMember(@Valid @RequestBody CreateMemberRequest request) {
        log.info("Received request to create member with email: {}", request.email());
        MemberResponse response = memberService.createMember(request);
        log.info("Successfully created member with ID: {}", response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Update member",
        description = "Update an existing member. Only accessible by users with ADMIN role. Cache is evicted after update."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Member successfully updated",
            content = @Content(schema = @Schema(implementation = MemberResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Member not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data or duplicate email",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - JWT token missing or invalid",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - ADMIN role required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<MemberResponse> updateMember(
            @Parameter(description = "Member UUID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMemberRequest request) {
        log.info("Received request to update member with ID: {}", id);
        MemberResponse response = memberService.updateMember(id, request);
        log.info("Successfully updated member with ID: {}", id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Delete member",
        description = "Delete a member by their ID. Only accessible by users with ADMIN role. Cache is evicted after deletion."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Member successfully deleted"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Member not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - JWT token missing or invalid",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - ADMIN role required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<Void> deleteMember(
            @Parameter(description = "Member UUID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id) {
        log.info("Received request to delete member with ID: {}", id);
        memberService.deleteMember(id);
        log.info("Successfully deleted member with ID: {}", id);
        return ResponseEntity.noContent().build();
    }
}
