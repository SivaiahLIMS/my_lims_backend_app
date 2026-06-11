package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.dto.role.AssignPermissionsRequest;
import com.sivayahealth.lims.dto.role.CreateRoleRequest;
import com.sivayahealth.lims.entity.Permission;
import com.sivayahealth.lims.entity.Role;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.RolePermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Roles & Permissions", description = "Role CRUD and permission assignment")
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;

    // ── Roles ─────────────────────────────────────────────────────────────────

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_VIEW')")
    @Operation(summary = "List all roles")
    public ResponseEntity<List<Role>> listRoles() {
        return ResponseEntity.ok(rolePermissionService.getAllRoles());
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    @Operation(summary = "Create a new role")
    public ResponseEntity<Role> createRole(@RequestBody CreateRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rolePermissionService.createRole(request));
    }

    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('ROLE_EDIT')")
    @Operation(summary = "Update a role")
    public ResponseEntity<Role> updateRole(
            @PathVariable Long id,
            @RequestBody CreateRoleRequest request) {
        return ResponseEntity.ok(rolePermissionService.updateRole(id, request));
    }

    @PatchMapping("/roles/{id}/deactivate")
    @PreAuthorize("hasAuthority('ROLE_DELETE')")
    @Operation(summary = "Deactivate a role (soft delete — marks role inactive)")
    public ResponseEntity<Void> deactivateRole(@PathVariable Long id) {
        rolePermissionService.deactivateRole(id);
        return ResponseEntity.noContent().build();
    }

    // ── Role Permissions ──────────────────────────────────────────────────────

    @GetMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_PERMISSION_MAP')")
    @Operation(summary = "Get permissions assigned to a role for the current tenant")
    public ResponseEntity<List<Permission>> getRolePermissions(
            @PathVariable Long id,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(rolePermissionService.getPermissionsForRole(u.getTenantId(), id));
    }

    @PutMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_PERMISSION_MAP')")
    @Operation(summary = "Replace all permissions for a role (full replacement)")
    public ResponseEntity<List<Permission>> replacePermissions(
            @PathVariable Long id,
            @RequestBody AssignPermissionsRequest request,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                rolePermissionService.replacePermissionsForRole(u.getTenantId(), id, request.permissionCodes()));
    }

    @PostMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_PERMISSION_MAP')")
    @Operation(summary = "Add permissions to a role (additive, no duplicates)")
    public ResponseEntity<Void> addPermissions(
            @PathVariable Long id,
            @RequestBody AssignPermissionsRequest request,
            @AuthenticationPrincipal LimsUserDetails u) {
        rolePermissionService.addPermissionsToRole(u.getTenantId(), id, request.permissionCodes());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/roles/{id}/permissions/{permissionCode}")
    @PreAuthorize("hasAuthority('ROLE_PERMISSION_MAP')")
    @Operation(summary = "Remove a single permission from a role")
    public ResponseEntity<Void> removePermission(
            @PathVariable Long id,
            @PathVariable String permissionCode,
            @AuthenticationPrincipal LimsUserDetails u) {
        rolePermissionService.removePermissionFromRole(u.getTenantId(), id, permissionCode);
        return ResponseEntity.noContent().build();
    }

    // ── Permission Catalog ────────────────────────────────────────────────────

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('ROLE_PERMISSION_MAP')")
    @Operation(summary = "List all available permissions in the system")
    public ResponseEntity<List<Permission>> listPermissions() {
        return ResponseEntity.ok(rolePermissionService.getAllPermissions());
    }
}
