package com.sivayahealth.lims.service;

import com.sivayahealth.lims.dto.role.CreateRoleRequest;
import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RolePermissionService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final TenantRolePermissionRepository tenantRolePermissionRepository;
    private final TenantRepository tenantRepository;
    private final AuditService auditService;

    @Transactional
    public void deactivateRole(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> LimsException.notFound("Role not found"));
        role.setActive(false);
        roleRepository.save(role);
    }

    @Transactional(readOnly = true)
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Transactional
    public Role createRole(CreateRoleRequest request) {
        if (roleRepository.existsByCode(request.code())) {
            throw LimsException.conflict("Role code already exists: " + request.code());
        }
        Role role = Role.builder()
                .code(request.code().toUpperCase())
                .name(request.name())
                .description(request.description())
                .build();
        return roleRepository.save(role);
    }

    @Transactional
    public Role updateRole(Long roleId, CreateRoleRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> LimsException.notFound("Role not found"));
        if (!role.getCode().equals(request.code()) && roleRepository.existsByCode(request.code())) {
            throw LimsException.conflict("Role code already exists: " + request.code());
        }
        role.setCode(request.code().toUpperCase());
        role.setName(request.name());
        role.setDescription(request.description());
        return roleRepository.save(role);
    }

    @Transactional(readOnly = true)
    public List<Permission> getPermissionsForRole(Long tenantId, Long roleId) {
        roleRepository.findById(roleId).orElseThrow(() -> LimsException.notFound("Role not found"));
        return tenantRolePermissionRepository.findByTenantIdAndRoleId(tenantId, roleId)
                .stream()
                .map(TenantRolePermission::getPermission)
                .toList();
    }

    @Transactional
    public List<Permission> replacePermissionsForRole(Long tenantId, Long roleId, List<String> permissionCodes) {
        roleRepository.findById(roleId).orElseThrow(() -> LimsException.notFound("Role not found"));
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> LimsException.notFound("Tenant not found"));
        Role role = roleRepository.findById(roleId).get();

        List<TenantRolePermission> existing = tenantRolePermissionRepository.findByTenantIdAndRoleId(tenantId, roleId);
        tenantRolePermissionRepository.deleteAll(existing);

        List<Permission> permissions = permissionCodes.stream()
                .map(code -> permissionRepository.findByCode(code)
                        .orElseThrow(() -> LimsException.notFound("Permission not found: " + code)))
                .toList();

        List<TenantRolePermission> newMappings = permissions.stream()
                .map(p -> TenantRolePermission.builder().tenant(tenant).role(role).permission(p).build())
                .toList();
        tenantRolePermissionRepository.saveAll(newMappings);
        auditService.log(tenantId, null, "RolePermission", roleId, "REPLACE_PERMISSIONS", null, String.valueOf(permissionCodes.size()));
        return permissions;
    }

    @Transactional
    public void addPermissionsToRole(Long tenantId, Long roleId, List<String> permissionCodes) {
        roleRepository.findById(roleId).orElseThrow(() -> LimsException.notFound("Role not found"));
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> LimsException.notFound("Tenant not found"));
        Role role = roleRepository.findById(roleId).get();

        for (String code : permissionCodes) {
            Permission perm = permissionRepository.findByCode(code)
                    .orElseThrow(() -> LimsException.notFound("Permission not found: " + code));
            TenantRolePermission trp = TenantRolePermission.builder()
                    .tenant(tenant).role(role).permission(perm).build();
            tenantRolePermissionRepository.save(trp);
        }
    }

    @Transactional
    public void removePermissionFromRole(Long tenantId, Long roleId, String permissionCode) {
        roleRepository.findById(roleId).orElseThrow(() -> LimsException.notFound("Role not found"));
        Permission perm = permissionRepository.findByCode(permissionCode)
                .orElseThrow(() -> LimsException.notFound("Permission not found: " + permissionCode));

        List<TenantRolePermission> existing = tenantRolePermissionRepository.findByTenantIdAndRoleId(tenantId, roleId);
        existing.stream()
                .filter(trp -> trp.getPermission().getId().equals(perm.getId()))
                .forEach(tenantRolePermissionRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }
}
