package com.autovision.platform.commercial;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface CommercialEnquiryRepository extends JpaRepository<CommercialEnquiry, UUID> {
    boolean existsByBusinessEmailAndPurposeAndProductAndCreatedAtAfter(
            String businessEmail, CommercialEnquiryPurpose purpose, CommercialProduct product, OffsetDateTime createdAt);
}
