package com.tietoevry.surestapp.service;

import com.tietoevry.surestapp.domain.Member;
import com.tietoevry.surestapp.dto.request.CreateMemberRequest;
import com.tietoevry.surestapp.dto.request.UpdateMemberRequest;
import com.tietoevry.surestapp.dto.response.MemberResponse;
import com.tietoevry.surestapp.dto.response.PagedMemberResponse;
import com.tietoevry.surestapp.exception.DuplicateEmailException;
import com.tietoevry.surestapp.exception.MemberNotFoundException;
import com.tietoevry.surestapp.repository.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class MemberService {

    private static final Logger logger = LoggerFactory.getLogger(MemberService.class);
    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public PagedMemberResponse getAllMembers(PageRequest pageRequest, String firstName, String lastName) {
        logger.debug("Fetching members with page: {}, firstName: {}, lastName: {}", pageRequest, firstName, lastName);
        Page<Member> memberPage;

        if (firstName != null || lastName != null) {
            String searchFirstName = firstName != null ? firstName : "";
            String searchLastName = lastName != null ? lastName : "";
            memberPage = memberRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                searchFirstName, searchLastName, pageRequest);
        } else {
            memberPage = memberRepository.findAll(pageRequest);
        }

        return new PagedMemberResponse(
            memberPage.getContent().stream().map(this::toResponse).toList(),
            memberPage.getNumber(),
            memberPage.getSize(),
            memberPage.getTotalElements(),
            memberPage.getTotalPages(),
            memberPage.isLast()
        );
    }

    @Cacheable(value = "members", key = "#id")
    @Transactional(readOnly = true)
    public MemberResponse getMemberById(UUID id) {
        logger.debug("Fetching member by id: {}", id);
        Member member = memberRepository.findById(id)
            .orElseThrow(() -> new MemberNotFoundException("Member not found with id: " + id));
        return toResponse(member);
    }

    @Transactional
    public MemberResponse createMember(CreateMemberRequest request) {
        logger.info("Creating new member with email: {}", request.email());
        if (memberRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException("Member already exists with email: " + request.email());
        }

        Member member = new Member(
            request.firstName(),
            request.lastName(),
            request.dateOfBirth(),
            request.email()
        );

        Member savedMember = memberRepository.save(member);
        logger.info("Successfully created member with id: {}", savedMember.getId());
        return toResponse(savedMember);
    }

    @CacheEvict(value = "members", key = "#id")
    @Transactional
    public MemberResponse updateMember(UUID id, UpdateMemberRequest request) {
        logger.info("Updating member with id: {}", id);
        Member member = memberRepository.findById(id)
            .orElseThrow(() -> new MemberNotFoundException("Member not found with id: " + id));

        if (!member.getEmail().equals(request.email()) &&
            memberRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException("Email already in use: " + request.email());
        }

        member.updateDetails(
            request.firstName(),
            request.lastName(),
            request.dateOfBirth(),
            request.email()
        );

        Member updatedMember = memberRepository.save(member);
        logger.info("Successfully updated member with id: {}", updatedMember.getId());
        return toResponse(updatedMember);
    }

    @CacheEvict(value = "members", key = "#id")
    @Transactional
    public void deleteMember(UUID id) {
        logger.info("Deleting member with id: {}", id);
        if (!memberRepository.existsById(id)) {
            throw new MemberNotFoundException("Member not found with id: " + id);
        }
        memberRepository.deleteById(id);
        logger.info("Successfully deleted member with id: {}", id);
    }

    private MemberResponse toResponse(Member member) {
        return new MemberResponse(
            member.getId(),
            member.getFirstName(),
            member.getLastName(),
            member.getDateOfBirth(),
            member.getEmail(),
            member.getCreatedAt(),
            member.getUpdatedAt()
        );
    }
}
