package com.tietoevry.surestapp.controller;

import com.tietoevry.surestapp.dto.request.CreateMemberRequest;
import com.tietoevry.surestapp.dto.request.UpdateMemberRequest;
import com.tietoevry.surestapp.dto.response.MemberResponse;
import com.tietoevry.surestapp.dto.response.PagedMemberResponse;
import com.tietoevry.surestapp.exception.DuplicateEmailException;
import com.tietoevry.surestapp.exception.MemberNotFoundException;
import com.tietoevry.surestapp.service.MemberService;
import com.tietoevry.surestapp.util.TestDataBuilder;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MemberController.
 * <p>
 * NOTE: @PreAuthorize annotations are not active in unit tests (no Spring Security context).
 * Authorization rules are tested in integration tests where Spring Security is available.
 * These unit tests focus on business logic and request/response handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MemberController Unit Tests")
class MemberControllerTest {

    @Mock
    private MemberService memberService;

    @InjectMocks
    private MemberController memberController;

    private Validator validator;

    private static final UUID TEST_MEMBER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final UUID NON_EXISTENT_ID = UUID.fromString("999e9999-e99b-99d9-a999-999999999999");
    private static final String TEST_EMAIL = "john.doe@example.com";
    private static final String DUPLICATE_EMAIL = "duplicate@example.com";
    private static final String INVALID_EMAIL = "not-an-email";
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";
    private static final LocalDate VALID_DOB = LocalDate.of(1990, 1, 1);
    private static final LocalDate FUTURE_DATE = LocalDate.now().plusDays(1);

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("GET /api/members - getAllMembers()")
    class GetAllMembersTests {

        @Test
        @DisplayName("Should return paged members when no filters provided")
        void should_returnPagedMembers_when_noFiltersProvided() {
            // Arrange
            List<MemberResponse> members = List.of(
                TestDataBuilder.memberResponse(),
                TestDataBuilder.memberResponseWith("Jane", "Smith", "jane.smith@example.com"),
                TestDataBuilder.memberResponseWith("Bob", "Johnson", "bob.johnson@example.com")
            );
            PagedMemberResponse pagedResponse = TestDataBuilder.pagedMemberResponse(members, 0, 20, 3);

            PageRequest expectedPageRequest = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            when(memberService.getAllMembers(any(PageRequest.class), isNull(), isNull()))
                .thenReturn(pagedResponse);

            // Act
            ResponseEntity<PagedMemberResponse> response = memberController.getAllMembers(0, 20, "createdAt", "DESC", null, null);

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(3, response.getBody().content().size());
            assertEquals(0, response.getBody().page());
            assertEquals(20, response.getBody().size());
            assertEquals(3, response.getBody().totalElements());

            ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);
            verify(memberService, times(1)).getAllMembers(pageRequestCaptor.capture(), isNull(), isNull());

            PageRequest capturedPageRequest = pageRequestCaptor.getValue();
            assertEquals(expectedPageRequest.getPageNumber(), capturedPageRequest.getPageNumber());
            assertEquals(expectedPageRequest.getPageSize(), capturedPageRequest.getPageSize());
        }

        @Test
        @DisplayName("Should return filtered members when firstName provided")
        void should_returnFilteredMembers_when_firstNameProvided() {
            // Arrange
            List<MemberResponse> members = List.of(TestDataBuilder.memberResponse());
            PagedMemberResponse pagedResponse = TestDataBuilder.singlePageResponse(members);
            when(memberService.getAllMembers(any(PageRequest.class), eq("John"), isNull()))
                .thenReturn(pagedResponse);

            // Act
            ResponseEntity<PagedMemberResponse> response = memberController.getAllMembers(0, 20, "createdAt", "DESC", "John", null);

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(memberService, times(1)).getAllMembers(any(PageRequest.class), eq("John"), isNull());
        }

        @Test
        @DisplayName("Should return filtered members when lastName provided")
        void should_returnFilteredMembers_when_lastNameProvided() {
            // Arrange
            List<MemberResponse> members = List.of(TestDataBuilder.memberResponse());
            PagedMemberResponse pagedResponse = TestDataBuilder.singlePageResponse(members);
            when(memberService.getAllMembers(any(PageRequest.class), isNull(), eq("Doe")))
                .thenReturn(pagedResponse);

            // Act
            ResponseEntity<PagedMemberResponse> response = memberController.getAllMembers(0, 20, "createdAt", "DESC", null, "Doe");

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(memberService, times(1)).getAllMembers(any(PageRequest.class), isNull(), eq("Doe"));
        }

        @Test
        @DisplayName("Should return filtered members when both names provided")
        void should_returnFilteredMembers_when_bothNamesProvided() {
            // Arrange
            List<MemberResponse> members = List.of(TestDataBuilder.memberResponse());
            PagedMemberResponse pagedResponse = TestDataBuilder.singlePageResponse(members);
            when(memberService.getAllMembers(any(PageRequest.class), eq("John"), eq("Doe")))
                .thenReturn(pagedResponse);

            // Act
            ResponseEntity<PagedMemberResponse> response = memberController.getAllMembers(0, 20, "createdAt", "DESC", "John", "Doe");

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(memberService, times(1)).getAllMembers(any(PageRequest.class), eq("John"), eq("Doe"));
        }

        @Test
        @DisplayName("Should apply sorting when sort parameters provided")
        void should_applySorting_when_sortParametersProvided() {
            // Arrange
            PagedMemberResponse pagedResponse = TestDataBuilder.emptyPagedResponse();
            when(memberService.getAllMembers(any(PageRequest.class), isNull(), isNull()))
                .thenReturn(pagedResponse);

            // Act
            ResponseEntity<PagedMemberResponse> response = memberController.getAllMembers(0, 20, "lastName", "ASC", null, null);

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());

            ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);
            verify(memberService, times(1)).getAllMembers(pageRequestCaptor.capture(), isNull(), isNull());

            PageRequest capturedPageRequest = pageRequestCaptor.getValue();
            assertEquals(Sort.by(Sort.Direction.ASC, "lastName"), capturedPageRequest.getSort());
        }

        @Test
        @DisplayName("Should return second page when page parameter is one")
        void should_returnSecondPage_when_pageParameterIsOne() {
            // Arrange
            PagedMemberResponse pagedResponse = TestDataBuilder.pagedMemberResponse(List.of(), 1, 10, 25);
            when(memberService.getAllMembers(any(PageRequest.class), isNull(), isNull()))
                .thenReturn(pagedResponse);

            // Act
            ResponseEntity<PagedMemberResponse> response = memberController.getAllMembers(1, 10, "createdAt", "DESC", null, null);

            // Assert
            assertNotNull(response);
            assertEquals(1, response.getBody().page());

            ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);
            verify(memberService, times(1)).getAllMembers(pageRequestCaptor.capture(), isNull(), isNull());

            PageRequest capturedPageRequest = pageRequestCaptor.getValue();
            assertEquals(1, capturedPageRequest.getPageNumber());
            assertEquals(10, capturedPageRequest.getPageSize());
        }

        @Test
        @DisplayName("Should return empty page when no members exist")
        void should_returnEmptyPage_when_noMembersExist() {
            // Arrange
            PagedMemberResponse emptyResponse = TestDataBuilder.emptyPagedResponse();
            when(memberService.getAllMembers(any(PageRequest.class), isNull(), isNull()))
                .thenReturn(emptyResponse);

            // Act
            ResponseEntity<PagedMemberResponse> response = memberController.getAllMembers(0, 20, "createdAt", "DESC", null, null);

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().content().isEmpty());
            assertEquals(0, response.getBody().totalElements());
        }

        @Test
        @DisplayName("Should handle default pagination when parameters not provided")
        void should_handleDefaultPagination_when_parametersNotProvided() {
            // Arrange
            PagedMemberResponse pagedResponse = TestDataBuilder.emptyPagedResponse();
            when(memberService.getAllMembers(any(PageRequest.class), isNull(), isNull()))
                .thenReturn(pagedResponse);

            // Act
            ResponseEntity<PagedMemberResponse> response = memberController.getAllMembers(0, 20, "createdAt", "DESC", null, null);

            // Assert
            ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);
            verify(memberService).getAllMembers(pageRequestCaptor.capture(), isNull(), isNull());

            PageRequest capturedPageRequest = pageRequestCaptor.getValue();
            assertEquals(0, capturedPageRequest.getPageNumber());
            assertEquals(20, capturedPageRequest.getPageSize());
        }

        @Test
        @DisplayName("Should handle descending sort when direction is DESC")
        void should_handleDescendingSort_when_directionIsDesc() {
            // Arrange
            PagedMemberResponse pagedResponse = TestDataBuilder.emptyPagedResponse();
            when(memberService.getAllMembers(any(PageRequest.class), isNull(), isNull()))
                .thenReturn(pagedResponse);

            // Act
            memberController.getAllMembers(0, 20, "createdAt", "DESC", null, null);

            // Assert
            ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);
            verify(memberService).getAllMembers(pageRequestCaptor.capture(), isNull(), isNull());

            PageRequest capturedPageRequest = pageRequestCaptor.getValue();
            assertEquals(Sort.Direction.DESC, capturedPageRequest.getSort().getOrderFor("createdAt").getDirection());
        }

        @Test
        @DisplayName("Should handle ascending sort when direction is ASC")
        void should_handleAscendingSort_when_directionIsAsc() {
            // Arrange
            PagedMemberResponse pagedResponse = TestDataBuilder.emptyPagedResponse();
            when(memberService.getAllMembers(any(PageRequest.class), isNull(), isNull()))
                .thenReturn(pagedResponse);

            // Act
            memberController.getAllMembers(0, 20, "firstName", "ASC", null, null);

            // Assert
            ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);
            verify(memberService).getAllMembers(pageRequestCaptor.capture(), isNull(), isNull());

            PageRequest capturedPageRequest = pageRequestCaptor.getValue();
            assertEquals(Sort.Direction.ASC, capturedPageRequest.getSort().getOrderFor("firstName").getDirection());
        }

        @Test
        @DisplayName("Should handle last page when requesting final page")
        void should_handleLastPage_when_requestingFinalPage() {
            // Arrange
            PagedMemberResponse lastPageResponse = TestDataBuilder.pagedMemberResponse(List.of(TestDataBuilder.memberResponse()), 2, 20, 45);
            when(memberService.getAllMembers(any(PageRequest.class), isNull(), isNull()))
                .thenReturn(lastPageResponse);

            // Act
            ResponseEntity<PagedMemberResponse> response = memberController.getAllMembers(2, 20, "createdAt", "DESC", null, null);

            // Assert
            assertNotNull(response);
            assertTrue(response.getBody().last());
        }

        @Test
        @DisplayName("Should handle custom page size when size parameter provided")
        void should_handleCustomPageSize_when_sizeParameterProvided() {
            // Arrange
            PagedMemberResponse pagedResponse = TestDataBuilder.pagedMemberResponse(List.of(), 0, 50, 100);
            when(memberService.getAllMembers(any(PageRequest.class), isNull(), isNull()))
                .thenReturn(pagedResponse);

            // Act
            memberController.getAllMembers(0, 50, "createdAt", "DESC", null, null);

            // Assert
            ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);
            verify(memberService).getAllMembers(pageRequestCaptor.capture(), isNull(), isNull());

            PageRequest capturedPageRequest = pageRequestCaptor.getValue();
            assertEquals(50, capturedPageRequest.getPageSize());
        }
    }

    @Nested
    @DisplayName("GET /api/members/{id} - getMemberById()")
    class GetMemberByIdTests {

        @Test
        @DisplayName("Should return member response when member exists")
        void should_returnMemberResponse_when_memberExists() {
            // Arrange
            MemberResponse expectedMember = TestDataBuilder.memberResponseWithId(TEST_MEMBER_ID);
            when(memberService.getMemberById(TEST_MEMBER_ID)).thenReturn(expectedMember);

            // Act
            ResponseEntity<MemberResponse> response = memberController.getMemberById(TEST_MEMBER_ID);

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(TEST_MEMBER_ID, response.getBody().id());
            assertEquals(expectedMember.firstName(), response.getBody().firstName());
            assertEquals(expectedMember.lastName(), response.getBody().lastName());
            assertEquals(expectedMember.email(), response.getBody().email());
            verify(memberService, times(1)).getMemberById(TEST_MEMBER_ID);
        }

        @Test
        @DisplayName("Should return member with all fields when member has complete data")
        void should_returnMemberWithAllFields_when_memberHasCompleteData() {
            // Arrange
            MemberResponse expectedMember = TestDataBuilder.memberResponse();
            when(memberService.getMemberById(any(UUID.class))).thenReturn(expectedMember);

            // Act
            ResponseEntity<MemberResponse> response = memberController.getMemberById(TEST_MEMBER_ID);

            // Assert
            assertNotNull(response.getBody());
            assertNotNull(response.getBody().id());
            assertNotNull(response.getBody().firstName());
            assertNotNull(response.getBody().lastName());
            assertNotNull(response.getBody().dateOfBirth());
            assertNotNull(response.getBody().email());
            assertNotNull(response.getBody().createdAt());
            assertNotNull(response.getBody().updatedAt());
        }

        @Test
        @DisplayName("Should throw MemberNotFoundException when member does not exist")
        void should_throwMemberNotFoundException_when_memberDoesNotExist() {
            // Arrange
            when(memberService.getMemberById(NON_EXISTENT_ID))
                .thenThrow(new MemberNotFoundException("Member not found with id: " + NON_EXISTENT_ID));

            // Act & Assert
            MemberNotFoundException exception = assertThrows(
                MemberNotFoundException.class,
                () -> memberController.getMemberById(NON_EXISTENT_ID)
            );
            assertTrue(exception.getMessage().contains(NON_EXISTENT_ID.toString()));
            verify(memberService, times(1)).getMemberById(NON_EXISTENT_ID);
        }

        @Test
        @DisplayName("Should throw MemberNotFoundException when id is random")
        void should_throwMemberNotFoundException_when_idIsRandom() {
            // Arrange
            UUID randomId = UUID.randomUUID();
            when(memberService.getMemberById(randomId))
                .thenThrow(new MemberNotFoundException("Member not found with id: " + randomId));

            // Act & Assert
            assertThrows(MemberNotFoundException.class, () -> memberController.getMemberById(randomId));
        }

        @Test
        @DisplayName("Should call service once when getMemberById invoked")
        void should_callServiceOnce_when_getMemberByIdInvoked() {
            // Arrange
            MemberResponse expectedMember = TestDataBuilder.memberResponse();
            when(memberService.getMemberById(TEST_MEMBER_ID)).thenReturn(expectedMember);

            // Act
            memberController.getMemberById(TEST_MEMBER_ID);

            // Assert
            verify(memberService, times(1)).getMemberById(TEST_MEMBER_ID);
        }
    }

    @Nested
    @DisplayName("POST /api/members - createMember()")
    class CreateMemberTests {

        @Test
        @DisplayName("Should return created member when request is valid")
        void should_returnCreatedMember_when_requestIsValid() {
            // Arrange
            CreateMemberRequest request = TestDataBuilder.validCreateMemberRequest();
            MemberResponse createdMember = TestDataBuilder.memberResponse();
            when(memberService.createMember(request)).thenReturn(createdMember);

            // Act
            ResponseEntity<MemberResponse> response = memberController.createMember(request);

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertNotNull(response.getBody().id());
            assertEquals(request.firstName(), response.getBody().firstName());
            assertEquals(request.lastName(), response.getBody().lastName());
            assertEquals(request.email(), response.getBody().email());
            verify(memberService, times(1)).createMember(request);
        }

        @Test
        @DisplayName("Should return member with timestamps when member created")
        void should_returnMemberWithTimestamps_when_memberCreated() {
            // Arrange
            CreateMemberRequest request = TestDataBuilder.validCreateMemberRequest();
            MemberResponse createdMember = TestDataBuilder.memberResponse();
            when(memberService.createMember(request)).thenReturn(createdMember);

            // Act
            ResponseEntity<MemberResponse> response = memberController.createMember(request);

            // Assert
            assertNotNull(response.getBody());
            assertNotNull(response.getBody().createdAt());
            assertNotNull(response.getBody().updatedAt());
        }

        @Test
        @DisplayName("Should throw DuplicateEmailException when email already exists")
        void should_throwDuplicateEmailException_when_emailAlreadyExists() {
            // Arrange
            CreateMemberRequest request = TestDataBuilder.createMemberRequestWithEmail(DUPLICATE_EMAIL);
            when(memberService.createMember(request))
                .thenThrow(new DuplicateEmailException("Member already exists with email: " + DUPLICATE_EMAIL));

            // Act & Assert
            DuplicateEmailException exception = assertThrows(
                DuplicateEmailException.class,
                () -> memberController.createMember(request)
            );
            assertTrue(exception.getMessage().contains(DUPLICATE_EMAIL));
            verify(memberService, times(1)).createMember(request);
        }

        @Test
        @DisplayName("Should throw DuplicateEmailException when creating second member with same email")
        void should_throwDuplicateEmailException_when_creatingSecondMemberWithSameEmail() {
            // Arrange
            CreateMemberRequest request = TestDataBuilder.createMemberRequestWithEmail(TEST_EMAIL);
            when(memberService.createMember(request))
                .thenThrow(new DuplicateEmailException("Member already exists with email: " + TEST_EMAIL));

            // Act & Assert
            assertThrows(DuplicateEmailException.class, () -> memberController.createMember(request));
        }

        @Test
        @DisplayName("Should fail validation when firstName is blank")
        void should_failValidation_when_firstNameIsBlank() {
            // Arrange - Using null instead of blank to avoid compact constructor validation
            CreateMemberRequest request = new CreateMemberRequest(null, LAST_NAME, VALID_DOB, TEST_EMAIL);

            // Act
            Set<ConstraintViolation<CreateMemberRequest>> violations = validator.validate(request);

            // Assert
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().equals("First name is required")));
        }

        @Test
        @DisplayName("Should fail validation when lastName is blank")
        void should_failValidation_when_lastNameIsBlank() {
            // Arrange - Using null instead of blank to avoid compact constructor validation
            CreateMemberRequest request = new CreateMemberRequest(FIRST_NAME, null, VALID_DOB, TEST_EMAIL);

            // Act
            Set<ConstraintViolation<CreateMemberRequest>> violations = validator.validate(request);

            // Assert
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().equals("Last name is required")));
        }

        @Test
        @DisplayName("Should fail validation when email is invalid")
        void should_failValidation_when_emailIsInvalid() {
            // Arrange
            CreateMemberRequest request = TestDataBuilder.createMemberRequestWith(FIRST_NAME, LAST_NAME, VALID_DOB, INVALID_EMAIL);

            // Act
            Set<ConstraintViolation<CreateMemberRequest>> violations = validator.validate(request);

            // Assert
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().equals("Email must be valid")));
        }

        @Test
        @DisplayName("Should fail validation when dateOfBirth is in future")
        void should_failValidation_when_dateOfBirthIsInFuture() {
            // Arrange
            CreateMemberRequest request = TestDataBuilder.createMemberRequestWith(FIRST_NAME, LAST_NAME, FUTURE_DATE, TEST_EMAIL);

            // Act
            Set<ConstraintViolation<CreateMemberRequest>> violations = validator.validate(request);

            // Assert
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().equals("Date of birth must be in the past")));
        }

        @Test
        @DisplayName("Should fail validation when email is blank")
        void should_failValidation_when_emailIsBlank() {
            // Arrange
            CreateMemberRequest request = TestDataBuilder.createMemberRequestWith(FIRST_NAME, LAST_NAME, VALID_DOB, "");

            // Act
            Set<ConstraintViolation<CreateMemberRequest>> violations = validator.validate(request);

            // Assert
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().equals("Email is required") || v.getMessage().equals("Email must be valid")));
        }
    }

    @Nested
    @DisplayName("PUT /api/members/{id} - updateMember()")
    class UpdateMemberTests {

        @Test
        @DisplayName("Should return updated member when request is valid")
        void should_returnUpdatedMember_when_requestIsValid() {
            // Arrange
            UpdateMemberRequest request = TestDataBuilder.validUpdateMemberRequest();
            MemberResponse updatedMember = TestDataBuilder.memberResponseWithId(TEST_MEMBER_ID);
            when(memberService.updateMember(TEST_MEMBER_ID, request)).thenReturn(updatedMember);

            // Act
            ResponseEntity<MemberResponse> response = memberController.updateMember(TEST_MEMBER_ID, request);

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(TEST_MEMBER_ID, response.getBody().id());
            assertEquals(request.firstName(), response.getBody().firstName());
            assertEquals(request.lastName(), response.getBody().lastName());
            verify(memberService, times(1)).updateMember(TEST_MEMBER_ID, request);
        }

        @Test
        @DisplayName("Should allow same email when updating own member")
        void should_allowSameEmail_when_updatingOwnMember() {
            // Arrange
            UpdateMemberRequest request = TestDataBuilder.updateMemberRequestWithEmail(TEST_EMAIL);
            MemberResponse updatedMember = TestDataBuilder.memberResponse();
            when(memberService.updateMember(TEST_MEMBER_ID, request)).thenReturn(updatedMember);

            // Act
            ResponseEntity<MemberResponse> response = memberController.updateMember(TEST_MEMBER_ID, request);

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(memberService, times(1)).updateMember(TEST_MEMBER_ID, request);
        }

        @Test
        @DisplayName("Should throw MemberNotFoundException when member does not exist")
        void should_throwMemberNotFoundException_when_memberDoesNotExist() {
            // Arrange
            UpdateMemberRequest request = TestDataBuilder.validUpdateMemberRequest();
            when(memberService.updateMember(NON_EXISTENT_ID, request))
                .thenThrow(new MemberNotFoundException("Member not found with id: " + NON_EXISTENT_ID));

            // Act & Assert
            MemberNotFoundException exception = assertThrows(
                MemberNotFoundException.class,
                () -> memberController.updateMember(NON_EXISTENT_ID, request)
            );
            assertTrue(exception.getMessage().contains(NON_EXISTENT_ID.toString()));
        }

        @Test
        @DisplayName("Should throw DuplicateEmailException when email belongs to another member")
        void should_throwDuplicateEmailException_when_emailBelongsToAnotherMember() {
            // Arrange
            UpdateMemberRequest request = TestDataBuilder.updateMemberRequestWithEmail(DUPLICATE_EMAIL);
            when(memberService.updateMember(TEST_MEMBER_ID, request))
                .thenThrow(new DuplicateEmailException("Email already in use: " + DUPLICATE_EMAIL));

            // Act & Assert
            DuplicateEmailException exception = assertThrows(
                DuplicateEmailException.class,
                () -> memberController.updateMember(TEST_MEMBER_ID, request)
            );
            assertTrue(exception.getMessage().contains(DUPLICATE_EMAIL));
        }

        @Test
        @DisplayName("Should fail validation when updateRequest has blank firstName")
        void should_failValidation_when_updateRequestHasBlankFirstName() {
            // Arrange
            UpdateMemberRequest request = TestDataBuilder.updateMemberRequestWith("", LAST_NAME, VALID_DOB, TEST_EMAIL);

            // Act
            Set<ConstraintViolation<UpdateMemberRequest>> violations = validator.validate(request);

            // Assert
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().equals("First name is required")));
        }

        @Test
        @DisplayName("Should fail validation when updateRequest has invalid email")
        void should_failValidation_when_updateRequestHasInvalidEmail() {
            // Arrange
            UpdateMemberRequest request = TestDataBuilder.updateMemberRequestWith(FIRST_NAME, LAST_NAME, VALID_DOB, INVALID_EMAIL);

            // Act
            Set<ConstraintViolation<UpdateMemberRequest>> violations = validator.validate(request);

            // Assert
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().equals("Email must be valid")));
        }
    }

    @Nested
    @DisplayName("DELETE /api/members/{id} - deleteMember()")
    class DeleteMemberTests {

        @Test
        @DisplayName("Should return no content when member deleted successfully")
        void should_returnNoContent_when_memberDeletedSuccessfully() {
            // Arrange
            doNothing().when(memberService).deleteMember(TEST_MEMBER_ID);

            // Act
            ResponseEntity<Void> response = memberController.deleteMember(TEST_MEMBER_ID);

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
            assertNull(response.getBody());
            verify(memberService, times(1)).deleteMember(TEST_MEMBER_ID);
        }

        @Test
        @DisplayName("Should invoke service delete when delete endpoint called")
        void should_invokeServiceDelete_when_deleteEndpointCalled() {
            // Arrange
            doNothing().when(memberService).deleteMember(TEST_MEMBER_ID);

            // Act
            memberController.deleteMember(TEST_MEMBER_ID);

            // Assert
            verify(memberService, times(1)).deleteMember(TEST_MEMBER_ID);
        }

        @Test
        @DisplayName("Should throw MemberNotFoundException when deleting non-existent member")
        void should_throwMemberNotFoundException_when_deletingNonExistentMember() {
            // Arrange
            doThrow(new MemberNotFoundException("Member not found with id: " + NON_EXISTENT_ID))
                .when(memberService).deleteMember(NON_EXISTENT_ID);

            // Act & Assert
            MemberNotFoundException exception = assertThrows(
                MemberNotFoundException.class,
                () -> memberController.deleteMember(NON_EXISTENT_ID)
            );
            assertTrue(exception.getMessage().contains(NON_EXISTENT_ID.toString()));
        }

        @Test
        @DisplayName("Should throw MemberNotFoundException when member already deleted")
        void should_throwMemberNotFoundException_when_memberAlreadyDeleted() {
            // Arrange
            UUID deletedId = UUID.randomUUID();
            doThrow(new MemberNotFoundException("Member not found with id: " + deletedId))
                .when(memberService).deleteMember(deletedId);

            // Act & Assert
            assertThrows(MemberNotFoundException.class, () -> memberController.deleteMember(deletedId));
        }

        @Test
        @DisplayName("Should not catch exceptions when service throws unexpected exception")
        void should_notCatchExceptions_when_serviceThrowsUnexpectedException() {
            // Arrange
            doThrow(new RuntimeException("Unexpected error"))
                .when(memberService).deleteMember(TEST_MEMBER_ID);

            // Act & Assert
            assertThrows(RuntimeException.class, () -> memberController.deleteMember(TEST_MEMBER_ID));
        }
    }
}
