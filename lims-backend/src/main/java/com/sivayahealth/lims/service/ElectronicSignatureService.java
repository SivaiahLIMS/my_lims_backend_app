package com.sivayahealth.lims.service;

import com.sivayahealth.lims.entity.AppUser;
import com.sivayahealth.lims.entity.ElectronicSignature;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.AppUserRepository;
import com.sivayahealth.lims.repository.ElectronicSignatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ElectronicSignatureService {

    private final ElectronicSignatureRepository signatureRepository;
    private final AppUserRepository             userRepository;
    private final PasswordEncoder               passwordEncoder;

    @Transactional
    public ElectronicSignature sign(Long userId,
                                    String password,
                                    String action,
                                    String entityType,
                                    Long entityId,
                                    String reason,
                                    String ipAddress) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw LimsException.badRequest("Invalid credentials — electronic signature rejected");
        }

        ElectronicSignature sig = ElectronicSignature.builder()
                .userId(userId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .reason(reason)
                .ipAddress(ipAddress)
                .signedAt(LocalDateTime.now())
                .build();

        return signatureRepository.save(sig);
    }

    @Transactional(readOnly = true)
    public List<ElectronicSignature> getSignaturesForEntity(String entityType, Long entityId) {
        return signatureRepository.findByEntityTypeAndEntityIdOrderBySignedAtDesc(entityType, entityId);
    }
}
