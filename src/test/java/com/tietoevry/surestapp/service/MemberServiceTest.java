package com.tietoevry.surestapp.service;

import com.tietoevry.surestapp.domain.Member;
import com.tietoevry.surestapp.dto.request.CreateMemberRequest;
import com.tietoevry.surestapp.dto.request.UpdateMemberRequest;
import com.tietoevry.surestapp.dto.response.MemberResponse;
import com.tietoevry.surestapp.dto.response.PagedMemberResponse;
import com.tietoevry.surestapp.exception.DuplicateEmailException;
import com.tietoevry.surestapp.exception.MemberNotFoundException;
import com.tietoevry.surestapp.repository.MemberRepository;
import com.tietoevry.surestapp.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService Unit Tests")
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberService memberService;

    private Member member1;
    private Member member2;
    private UUID memberId;

    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";
    private static final LocalDate DATE_OF_BIRTH = LocalDate.of(1990, 1, 1);
    private static final String EMAIL = "john.doe@example.com";
    private static final String NEW_EMAIL = "jane.doe@example.com";

    @BeforeEach
    void setUp() {
        memberId = UUID.randomUUID();

        member1 = new Member(FIRST_NAME, LAST_NAME, DATE_OF_BIRTH, EMAIL);
        member2 = new Member("Jane", "Smith", LocalDate.of(1985, 5, 15), "jane.smith@example.com");
    }

    // A. getAllMembers() - Success Scenarios

    @Test
    @DisplayName("Should return paged members when no filters are applied")
    void should_returnPagedMembers_when_noFiltersApplied() {
        // Arrange
        PageRequest pageRequest = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Member> memberPage = new PageImpl<>(List.of(member1, member2), pageRequest, 2);
        when(memberRepository.findAll(pageRequest)).thenReturn(memberPage);

        // Act
        PagedMemberResponse response = memberService.getAllMembers(pageRequest, null, null);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.content().size());
        assertEquals(0, response.page());
        assertEquals(20, response.size());
        assertEquals(2, response.totalElements());
        assertEquals(1, response.totalPages());
        assertTrue(response.last());

        verify(memberRepository, times(1)).findAll(pageRequest);
        verify(memberRepository, never()).findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            anyString(), anyString(), any(PageRequest.class));
    }

    @Test
    @DisplayName("Should return filtered members when firstName filter is applied")
    void should_returnFilteredMembers_when_firstNameFilterApplied() {
        // Arrange
        PageRequest pageRequest = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Member> memberPage = new PageImpl<>(List.of(member1), pageRequest, 1);
        when(memberRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            "John", "", pageRequest)).thenReturn(memberPage);

        // Act
        PagedMemberResponse response = memberService.getAllMembers(pageRequest, "John", null);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals(FIRST_NAME, response.content().get(0).firstName());

        verify(memberRepository, times(1))
            .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase("John", "", pageRequest);
        verify(memberRepository, never()).findAll(any(PageRequest.class));
    }

    @Test
    @DisplayName("Should return filtered members when lastName filter is applied")
    void should_returnFilteredMembers_when_lastNameFilterApplied() {
        // Arrange
        PageRequest pageRequest = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Member> memberPage = new PageImpl<>(List.of(member2), pageRequest, 1);
        when(memberRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            "", "Smith", pageRequest)).thenReturn(memberPage);

        // Act
        PagedMemberResponse response = memberService.getAllMembers(pageRequest, null, "Smith");

        // Assert
        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals("Smith", response.content().get(0).lastName());

        verify(memberRepository, times(1))
            .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase("", "Smith", pageRequest);
    }

    @Test
    @DisplayName("Should return filtered members when both firstName and lastName filters are applied")
    void should_returnFilteredMembers_when_bothFiltersApplied() {
        // Arrange
        PageRequest pageRequest = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Member> memberPage = new PageImpl<>(List.of(member1), pageRequest, 1);
        when(memberRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            "John", "Doe", pageRequest)).thenReturn(memberPage);

        // Act
        PagedMemberResponse response = memberService.getAllMembers(pageRequest, "John", "Doe");

        // Assert
        assertNotNull(response);
        assertEquals(1, response.content().size());

        verify(memberRepository, times(1))
            .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase("John", "Doe", pageRequest);
    }

    @Test
    @DisplayName("Should return empty page when no members match filters")
    void should_returnEmptyPage_when_noMembersMatchFilters() {
        // Arrange
        PageRequest pageRequest = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Member> emptyPage = new PageImpl<>(List.of(), pageRequest, 0);
        when(memberRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            "Nonexistent", "", pageRequest)).thenReturn(emptyPage);

        // Act
        PagedMemberResponse response = memberService.getAllMembers(pageRequest, "Nonexistent", null);

        // Assert
        assertNotNull(response);
        assertTrue(response.content().isEmpty());
        assertEquals(0, response.totalElements());
    }

    // B. getMemberById() - Success and Exception Scenarios

    @Test
    @DisplayName("Should return member response when member exists")
    void should_returnMemberResponse_when_memberExists() {
        // Arrange
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member1));

        // Act
        MemberResponse response = memberService.getMemberById(memberId);

        // Assert
        assertNotNull(response);
        assertEquals(FIRST_NAME, response.firstName());
        assertEquals(LAST_NAME, response.lastName());
        assertEquals(EMAIL, response.email());
        assertEquals(DATE_OF_BIRTH, response.dateOfBirth());

        verify(memberRepository, times(1)).findById(memberId);
    }

    @Test
    @DisplayName("Should throw MemberNotFoundException when member does not exist")
    void should_throwMemberNotFoundException_when_memberDoesNotExist() {
        // Arrange
        UUID nonexistentId = UUID.randomUUID();
        when(memberRepository.findById(nonexistentId)).thenReturn(Optional.empty());

        // Act & Assert
        MemberNotFoundException exception = assertThrows(
            MemberNotFoundException.class,
            () -> memberService.getMemberById(nonexistentId)
        );

        assertEquals("Member not found with id: " + nonexistentId, exception.getMessage());
        verify(memberRepository, times(1)).findById(nonexistentId);
    }

    // C. createMember() - Success and Exception Scenarios

    @Test
    @DisplayName("Should create member when email is unique")
    void should_createMember_when_emailIsUnique() {
        // Arrange
        CreateMemberRequest request = TestDataBuilder.validCreateMemberRequest();
        when(memberRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        MemberResponse response = memberService.createMember(request);

        // Assert
        assertNotNull(response);
        assertEquals(FIRST_NAME, response.firstName());
        assertEquals(LAST_NAME, response.lastName());
        assertEquals(EMAIL, response.email());
        assertEquals(DATE_OF_BIRTH, response.dateOfBirth());

        verify(memberRepository, times(1)).existsByEmail(EMAIL);
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    @Test
    @DisplayName("Should throw DuplicateEmailException when email already exists")
    void should_throwDuplicateEmailException_when_emailAlreadyExists() {
        // Arrange
        CreateMemberRequest request = TestDataBuilder.validCreateMemberRequest();
        when(memberRepository.existsByEmail(EMAIL)).thenReturn(true);

        // Act & Assert
        DuplicateEmailException exception = assertThrows(
            DuplicateEmailException.class,
            () -> memberService.createMember(request)
        );

        assertEquals("Member already exists with email: " + EMAIL, exception.getMessage());
        verify(memberRepository, times(1)).existsByEmail(EMAIL);
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("Should save member with correct details when creating")
    void should_saveMemberWithCorrectDetails_when_creating() {
        // Arrange
        CreateMemberRequest request = TestDataBuilder.createMemberRequestWith(
            "Alice", "Brown", LocalDate.of(1995, 3, 20), "alice.brown@example.com");
        when(memberRepository.existsByEmail("alice.brown@example.com")).thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        memberService.createMember(request);

        // Assert
        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        Member savedMember = memberCaptor.getValue();

        assertEquals("Alice", savedMember.getFirstName());
        assertEquals("Brown", savedMember.getLastName());
        assertEquals(LocalDate.of(1995, 3, 20), savedMember.getDateOfBirth());
        assertEquals("alice.brown@example.com", savedMember.getEmail());
    }

    // D. updateMember() - Success and Exception Scenarios

    @Test
    @DisplayName("Should update member when member exists and email is unchanged")
    void should_updateMember_when_memberExistsAndEmailUnchanged() {
        // Arrange
        UpdateMemberRequest request = TestDataBuilder.updateMemberRequestWith(
            "John", "Doe", DATE_OF_BIRTH, EMAIL);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member1));
        when(memberRepository.save(member1)).thenReturn(member1);

        // Act
        MemberResponse response = memberService.updateMember(memberId, request);

        // Assert
        assertNotNull(response);
        assertEquals(FIRST_NAME, response.firstName());
        assertEquals(LAST_NAME, response.lastName());
        assertEquals(EMAIL, response.email());

        verify(memberRepository, times(1)).findById(memberId);
        verify(memberRepository, never()).existsByEmail(anyString());
        verify(memberRepository, times(1)).save(member1);
    }

    @Test
    @DisplayName("Should update member when email is changed to unique email")
    void should_updateMember_when_emailChangedToUniqueEmail() {
        // Arrange
        UpdateMemberRequest request = TestDataBuilder.updateMemberRequestWithEmail(NEW_EMAIL);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member1));
        when(memberRepository.existsByEmail(NEW_EMAIL)).thenReturn(false);
        when(memberRepository.save(member1)).thenReturn(member1);

        // Act
        MemberResponse response = memberService.updateMember(memberId, request);

        // Assert
        assertNotNull(response);
        assertEquals(NEW_EMAIL, response.email());

        verify(memberRepository, times(1)).findById(memberId);
        verify(memberRepository, times(1)).existsByEmail(NEW_EMAIL);
        verify(memberRepository, times(1)).save(member1);
    }

    @Test
    @DisplayName("Should throw MemberNotFoundException when updating nonexistent member")
    void should_throwMemberNotFoundException_when_updatingNonexistentMember() {
        // Arrange
        UUID nonexistentId = UUID.randomUUID();
        UpdateMemberRequest request = TestDataBuilder.validUpdateMemberRequest();
        when(memberRepository.findById(nonexistentId)).thenReturn(Optional.empty());

        // Act & Assert
        MemberNotFoundException exception = assertThrows(
            MemberNotFoundException.class,
            () -> memberService.updateMember(nonexistentId, request)
        );

        assertEquals("Member not found with id: " + nonexistentId, exception.getMessage());
        verify(memberRepository, times(1)).findById(nonexistentId);
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("Should throw DuplicateEmailException when updating to existing email")
    void should_throwDuplicateEmailException_when_updatingToExistingEmail() {
        // Arrange
        UpdateMemberRequest request = TestDataBuilder.updateMemberRequestWithEmail(NEW_EMAIL);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member1));
        when(memberRepository.existsByEmail(NEW_EMAIL)).thenReturn(true);

        // Act & Assert
        DuplicateEmailException exception = assertThrows(
            DuplicateEmailException.class,
            () -> memberService.updateMember(memberId, request)
        );

        assertEquals("Email already in use: " + NEW_EMAIL, exception.getMessage());
        verify(memberRepository, times(1)).findById(memberId);
        verify(memberRepository, times(1)).existsByEmail(NEW_EMAIL);
        verify(memberRepository, never()).save(any(Member.class));
    }

    // E. deleteMember() - Success and Exception Scenarios

    @Test
    @DisplayName("Should delete member when member exists")
    void should_deleteMember_when_memberExists() {
        // Arrange
        when(memberRepository.existsById(memberId)).thenReturn(true);
        doNothing().when(memberRepository).deleteById(memberId);

        // Act
        memberService.deleteMember(memberId);

        // Assert
        verify(memberRepository, times(1)).existsById(memberId);
        verify(memberRepository, times(1)).deleteById(memberId);
    }

    @Test
    @DisplayName("Should throw MemberNotFoundException when deleting nonexistent member")
    void should_throwMemberNotFoundException_when_deletingNonexistentMember() {
        // Arrange
        UUID nonexistentId = UUID.randomUUID();
        when(memberRepository.existsById(nonexistentId)).thenReturn(false);

        // Act & Assert
        MemberNotFoundException exception = assertThrows(
            MemberNotFoundException.class,
            () -> memberService.deleteMember(nonexistentId)
        );

        assertEquals("Member not found with id: " + nonexistentId, exception.getMessage());
        verify(memberRepository, times(1)).existsById(nonexistentId);
        verify(memberRepository, never()).deleteById(any(UUID.class));
    }

    @Test
    @DisplayName("Should call deleteById exactly once when deleting existing member")
    void should_callDeleteByIdOnce_when_deletingExistingMember() {
        // Arrange
        when(memberRepository.existsById(memberId)).thenReturn(true);
        doNothing().when(memberRepository).deleteById(memberId);

        // Act
        memberService.deleteMember(memberId);

        // Assert
        verify(memberRepository, times(1)).deleteById(memberId);
        verifyNoMoreInteractions(memberRepository);
    }
}
