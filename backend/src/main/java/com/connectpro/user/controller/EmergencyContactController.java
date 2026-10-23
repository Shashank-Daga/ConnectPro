package com.connectpro.user.controller;

import com.connectpro.user.dto.CreateEmergencyContactRequest;
import com.connectpro.user.dto.EmergencyContactResponse;
import com.connectpro.user.dto.UpdateEmergencyContactRequest;
import com.connectpro.user.service.EmergencyContactService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/emergency-contacts")
public class EmergencyContactController {

    private final EmergencyContactService emergencyContactService;

    public EmergencyContactController(
            EmergencyContactService emergencyContactService
    ) {
        this.emergencyContactService = emergencyContactService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmergencyContactResponse create(
            Authentication authentication,
            @Valid @RequestBody CreateEmergencyContactRequest request
    ) {
        return emergencyContactService.create(
                authentication,
                request
        );
    }

    @GetMapping
    public List<EmergencyContactResponse> getAll(
            Authentication authentication
    ) {
        return emergencyContactService.getAll(authentication);
    }

    @GetMapping("/{contactId}")
    public EmergencyContactResponse getById(
            Authentication authentication,
            @PathVariable UUID contactId
    ) {
        return emergencyContactService.getById(
                authentication,
                contactId
        );
    }

    @PatchMapping("/{contactId}")
    public EmergencyContactResponse update(
            Authentication authentication,
            @PathVariable UUID contactId,
            @Valid @RequestBody UpdateEmergencyContactRequest request
    ) {
        return emergencyContactService.update(
                authentication,
                contactId,
                request
        );
    }

    @DeleteMapping("/{contactId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            Authentication authentication,
            @PathVariable UUID contactId
    ) {
        emergencyContactService.delete(
                authentication,
                contactId
        );
    }

    @PostMapping("/{contactId}/activate")
    public EmergencyContactResponse activate(
            Authentication authentication,
            @PathVariable UUID contactId
    ) {
        return emergencyContactService.activate(
                authentication,
                contactId
        );
    }

    @PostMapping("/{contactId}/deactivate")
    public EmergencyContactResponse deactivate(
            Authentication authentication,
            @PathVariable UUID contactId
    ) {
        return emergencyContactService.deactivate(
                authentication,
                contactId
        );
    }
}
