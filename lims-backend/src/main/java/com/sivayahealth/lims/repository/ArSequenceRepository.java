package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.ArSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArSequenceRepository extends JpaRepository<ArSequence, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ArSequence> findByTenantIdAndYear(Long tenantId, Integer year);
}
