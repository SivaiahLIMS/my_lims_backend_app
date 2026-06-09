package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.ElectronicSignature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ElectronicSignatureRepository extends JpaRepository<ElectronicSignature, Long> {

    List<ElectronicSignature> findByEntityTypeAndEntityIdOrderBySignedAtDesc(
            String entityType, Long entityId);

    List<ElectronicSignature> findByUserIdOrderBySignedAtDesc(Long userId);
}
