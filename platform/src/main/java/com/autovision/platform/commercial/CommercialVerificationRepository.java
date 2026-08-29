package com.autovision.platform.commercial;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CommercialVerificationRepository extends JpaRepository<CommercialVerification, UUID> {
    Optional<CommercialVerification> findByTokenDigest(String tokenDigest);
}
