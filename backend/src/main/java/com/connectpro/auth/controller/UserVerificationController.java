package com.connectpro.auth.controller;

import com.connectpro.auth.dto.VerifyEmailRequest;
import com.connectpro.auth.dto.VerifyPhoneRequest;
import com.connectpro.auth.service.UserVerificationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/verify")
public class UserVerificationController {

    private final UserVerificationService userVerificationService;

    public UserVerificationController(
            UserVerificationService userVerificationService
    ) {
        this.userVerificationService = userVerificationService;
    }

    @PostMapping("/phone")
    public void verifyPhone(
            @Valid @RequestBody VerifyPhoneRequest request, HttpServletRequest httpRequest
    ) {
        userVerificationService.verifyPhone(
                request.phoneNumber(),
                request.verificationCode(),
                httpRequest
        );
    }

    @PostMapping("/email")
    public void verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request,  HttpServletRequest httpRequest
    ) {
        userVerificationService.verifyEmail(
                request.email(),
                request.verificationCode(),
                httpRequest
        );
    }
}
