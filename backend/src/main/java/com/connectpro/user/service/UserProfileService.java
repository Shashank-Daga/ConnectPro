package com.connectpro.user.service;

import com.connectpro.common.exception.ResourceNotFoundException;
import com.connectpro.common.security.AuthenticatedUser;
import com.connectpro.user.dto.UserProfileResponse;
import com.connectpro.user.entity.User;
import com.connectpro.user.repository.UserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserProfileService {

    private final UserRepository userRepository;

    public UserProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUser(
            Authentication authentication
    ) {
        UUID userId = AuthenticatedUser.getUserId(authentication);

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        return new UserProfileResponse(
                user.getId(),
                user.getPhoneNumber(),
                user.getEmail(),
                user.getStatus(),
                user.getPhoneVerifiedAt(),
                user.getEmailVerifiedAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
