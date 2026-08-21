package com.autovision.platform.aftersales;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerAuthorizationRepository
        extends JpaRepository<CustomerAuthorization, UUID> {

    List<CustomerAuthorization> findAllByTenantId(
            UUID tenantId
    );

    List<CustomerAuthorization> findAllByTenantIdAndAftersalesCaseId(
            UUID tenantId,
            UUID aftersalesCaseId
    );

    List<CustomerAuthorization>
    findAllByTenantIdAndServiceQuoteIdOrderByRequestedAtDescIdDesc(
            UUID tenantId,
            UUID serviceQuoteId
    );

    List<CustomerAuthorization> findAllByTenantIdAndServiceQuoteIdIn(
            UUID tenantId,
            List<UUID> serviceQuoteIds
    );

    List<CustomerAuthorization> findAllByTenantIdAndAuthorizationStatus(
            UUID tenantId,
            CustomerAuthorizationStatus authorizationStatus
    );

    Optional<CustomerAuthorization> findByIdAndTenantId(
            UUID id,
            UUID tenantId
    );

    Optional<CustomerAuthorization> findByTenantIdAndAuthorizationNumber(
            UUID tenantId,
            String authorizationNumber
    );

    boolean existsByTenantIdAndAuthorizationNumber(
            UUID tenantId,
            String authorizationNumber
    );

    boolean existsByTenantIdAndServiceQuoteIdAndAuthorizationStatus(
            UUID tenantId,
            UUID serviceQuoteId,
            CustomerAuthorizationStatus authorizationStatus
    );
}
