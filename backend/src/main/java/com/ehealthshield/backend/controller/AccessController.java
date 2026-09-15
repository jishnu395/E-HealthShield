package com.ehealthshield.backend.controller;

import com.ehealthshield.backend.dto.request.GrantAccessRequest;
import com.ehealthshield.backend.dto.request.RevokeAccessRequest;
import com.ehealthshield.backend.dto.response.AccessCheckResponse;
import com.ehealthshield.backend.dto.response.AccessPermissionResponse;
import com.ehealthshield.backend.exception.ValidationException;
import com.ehealthshield.backend.security.UserPrincipal;
import com.ehealthshield.backend.service.AclService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/access")
public class AccessController {

    private final AclService aclService;

    public AccessController(AclService aclService) {
        this.aclService = Objects.requireNonNull(aclService, "AclService must not be null");
    }

    /**
     * Patient grants a doctor access to an EHR record.
     */
    @PostMapping("/grant")
    public ResponseEntity<AccessPermissionResponse> grantAccess(
            @RequestBody GrantAccessRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "patientWallet", required = false) String patientWalletParam) {

        if (request == null || request.recordId() == null || request.doctorWallet() == null) {
            throw new ValidationException("recordId and doctorWallet are required");
        }

        String patientWallet = principal != null ? principal.getWalletAddress() : patientWalletParam;
        if (patientWallet == null || patientWallet.isBlank()) {
            throw new ValidationException("Authenticated patient identity or patientWallet parameter is required");
        }

        AccessPermissionResponse response = aclService.grantAccess(request.recordId(), request.doctorWallet(), patientWallet);
        return ResponseEntity.ok(response);
    }

    /**
     * Patient revokes a doctor's access to an EHR record.
     */
    @PostMapping("/revoke")
    public ResponseEntity<AccessPermissionResponse> revokeAccess(
            @RequestBody RevokeAccessRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "patientWallet", required = false) String patientWalletParam) {

        if (request == null || request.recordId() == null || request.doctorWallet() == null) {
            throw new ValidationException("recordId and doctorWallet are required");
        }

        String patientWallet = principal != null ? principal.getWalletAddress() : patientWalletParam;
        if (patientWallet == null || patientWallet.isBlank()) {
            throw new ValidationException("Authenticated patient identity or patientWallet parameter is required");
        }

        AccessPermissionResponse response = aclService.revokeAccess(request.recordId(), request.doctorWallet(), patientWallet);
        return ResponseEntity.ok(response);
    }

    /**
     * Checks if a doctor currently has access to an EHR record.
     */
    @GetMapping("/check")
    public ResponseEntity<AccessCheckResponse> checkAccess(
            @RequestParam("recordId") UUID recordId,
            @RequestParam("doctorWallet") String doctorWallet) {

        boolean hasAccess = aclService.checkAccess(recordId, doctorWallet);
        return ResponseEntity.ok(new AccessCheckResponse(recordId, doctorWallet, hasAccess));
    }

    /**
     * Patient inspects the access list for a specific record.
     */
    @GetMapping("/record/{recordId}")
    public ResponseEntity<List<AccessPermissionResponse>> getRecordPermissions(
            @PathVariable("recordId") UUID recordId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "patientWallet", required = false) String patientWalletParam) {

        String patientWallet = principal != null ? principal.getWalletAddress() : patientWalletParam;
        if (patientWallet == null || patientWallet.isBlank()) {
            throw new ValidationException("Authenticated patient identity or patientWallet parameter is required");
        }

        List<AccessPermissionResponse> permissions = aclService.getPermissionsForRecord(recordId, patientWallet);
        return ResponseEntity.ok(permissions);
    }

    /**
     * Patient inspects all permissions granted across their records.
     */
    @GetMapping("/patient/{patientWallet}")
    public ResponseEntity<List<AccessPermissionResponse>> getPatientPermissions(
            @PathVariable("patientWallet") String patientWallet) {
        if (patientWallet == null || patientWallet.isBlank()) {
            throw new ValidationException("patientWallet is required");
        }
        List<AccessPermissionResponse> permissions = aclService.getPermissionsForPatient(patientWallet);
        return ResponseEntity.ok(permissions);
    }
}
