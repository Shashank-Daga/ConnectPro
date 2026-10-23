package com.connectpro.user.service;

import com.connectpro.common.exception.ResourceNotFoundException;
import com.connectpro.common.security.AuthenticatedUser;
import com.connectpro.user.dto.CreateEmergencyContactRequest;
import com.connectpro.user.dto.EmergencyContactResponse;
import com.connectpro.user.dto.UpdateEmergencyContactRequest;
import com.connectpro.user.entity.EmergencyContact;
import com.connectpro.user.entity.User;
import com.connectpro.user.repository.EmergencyContactRepository;
import com.connectpro.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class EmergencyContactService {

    private final EmergencyContactRepository emergencyContactRepository;
    private final UserRepository userRepository;

    public EmergencyContactService(
            EmergencyContactRepository emergencyContactRepository,
            UserRepository userRepository
    ) {
        this.emergencyContactRepository = emergencyContactRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public EmergencyContactResponse create(
            Authentication authentication,
            CreateEmergencyContactRequest request
    ) {
        UUID userId = AuthenticatedUser.getUserId(authentication);

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        EmergencyContact contact = new EmergencyContact(
                user,
                request.name().trim(),
                request.phoneNumber().trim(),
                request.relationship().trim()
        );

        return toResponse(
                emergencyContactRepository.save(contact)
        );
    }

    @Transactional(readOnly = true)
    public List<EmergencyContactResponse> getAll(
            Authentication authentication
    ) {
        UUID userId = AuthenticatedUser.getUserId(authentication);

        return emergencyContactRepository
                .findAllByUserIdOrderByCreatedAtAsc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmergencyContactResponse getById(
            Authentication authentication,
            UUID contactId
    ) {
        UUID userId = AuthenticatedUser.getUserId(authentication);

        EmergencyContact contact = findOwnedContact(
                userId,
                contactId
        );

        return toResponse(contact);
    }

    @Transactional
    public EmergencyContactResponse update(
            Authentication authentication,
            UUID contactId,
            UpdateEmergencyContactRequest request
    ) {
        UUID userId = AuthenticatedUser.getUserId(authentication);

        EmergencyContact contact = findOwnedContact(
                userId,
                contactId
        );

        contact.update(
                request.name().trim(),
                request.phoneNumber().trim(),
                request.relationship().trim()
        );

        return toResponse(contact);
    }

    @Transactional
    public void delete(
            Authentication authentication,
            UUID contactId
    ) {
        UUID userId = AuthenticatedUser.getUserId(authentication);

        EmergencyContact contact = findOwnedContact(
                userId,
                contactId
        );

        emergencyContactRepository.delete(contact);
    }

    @Transactional
    public EmergencyContactResponse activate(
            Authentication authentication,
            UUID contactId
    ) {
        UUID userId = AuthenticatedUser.getUserId(authentication);

        EmergencyContact contact = findOwnedContact(
                userId,
                contactId
        );

        contact.activate();

        return toResponse(contact);
    }

    @Transactional
    public EmergencyContactResponse deactivate(
            Authentication authentication,
            UUID contactId
    ) {
        UUID userId = AuthenticatedUser.getUserId(authentication);

        EmergencyContact contact = findOwnedContact(
                userId,
                contactId
        );

        contact.deactivate();

        return toResponse(contact);
    }

    private EmergencyContact findOwnedContact(
            UUID userId,
            UUID contactId
    ) {
        return emergencyContactRepository
                .findByIdAndUserId(contactId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Emergency contact not found"
                        ));
    }

    private EmergencyContactResponse toResponse(
            EmergencyContact contact
    ) {
        return new EmergencyContactResponse(
                contact.getId(),
                contact.getName(),
                contact.getPhoneNumber(),
                contact.getRelationship(),
                contact.getStatus(),
                contact.getCreatedAt(),
                contact.getUpdatedAt()
        );
    }
}
