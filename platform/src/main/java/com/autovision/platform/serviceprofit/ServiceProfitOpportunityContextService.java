package com.autovision.platform.serviceprofit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class ServiceProfitOpportunityContextService {

    private final ServiceProfitOpportunityContextRepository repository;

    public ServiceProfitOpportunityContextService(
            ServiceProfitOpportunityContextRepository repository
    ) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<ServiceProfitOpportunityContext> findFor(
            ServiceProfitOpportunity opportunity
    ) {
        requireOpportunity(opportunity);
        return repository.findByOpportunityIdAndTenantId(
                opportunity.getId(),
                opportunity.getTenantId()
        );
    }

    @Transactional
    public ServiceProfitOpportunityContext capture(
            ServiceProfitOpportunity opportunity,
            ServiceProfitOpportunityContextSnapshot snapshot,
            OffsetDateTime capturedAt
    ) {
        requireOpportunity(opportunity);
        if (snapshot == null || capturedAt == null) {
            throw new IllegalArgumentException(
                    "Context snapshot and capture time are required"
            );
        }

        ServiceProfitOpportunityContext context = repository
                .findByOpportunityIdAndTenantId(
                        opportunity.getId(),
                        opportunity.getTenantId()
                )
                .orElseGet(() ->
                        ServiceProfitOpportunityContext.capture(
                                UUID.randomUUID(),
                                opportunity.getTenantId(),
                                opportunity,
                                snapshot,
                                capturedAt
                        )
                );

                context.refresh(snapshot, capturedAt);

        return repository.save(context);
    }

    private void requireOpportunity(
            ServiceProfitOpportunity opportunity
    ) {
        if (opportunity == null
                || opportunity.getId() == null
                || opportunity.getTenantId() == null) {
            throw new IllegalArgumentException(
                    "Service Profit opportunity is required"
            );
        }
    }
}