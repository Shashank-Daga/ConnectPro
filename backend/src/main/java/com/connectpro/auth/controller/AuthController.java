package com.connectpro.auth.controller;

import com.connectpro.auth.dto.AuthResponse;
import com.connectpro.auth.dto.LoginRequest;
import com.connectpro.auth.dto.RefreshTokenRequest;
import com.connectpro.auth.dto.RegisterRequest;
import com.connectpro.auth.dto.RequestPinResetRequest;
import com.connectpro.auth.dto.ResetPinRequest;
import com.connectpro.auth.service.AuthenticationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        authenticationService.register(request, httpRequest);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        AuthResponse response = authenticationService.login(request, httpRequest);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest
    ) {
        AuthResponse response = authenticationService.refresh(request, httpRequest);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }

    @PostMapping("/pin-reset/request")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void requestPinReset(
            @Valid @RequestBody RequestPinResetRequest request,
            HttpServletRequest httpRequest
    ) {
        authenticationService.requestPinReset(request, httpRequest);
    }

    @PostMapping("/pin-reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPin(
            @Valid @RequestBody ResetPinRequest request,
            HttpServletRequest httpRequest
    ) {
        authenticationService.resetPin(request, httpRequest);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(Authentication authentication) {
        authenticationService.logout(authentication);
    }
}
