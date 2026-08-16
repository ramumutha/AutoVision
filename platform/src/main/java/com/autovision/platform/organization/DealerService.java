package com.autovision.platform.organization;

import java.util.List;
import java.util.UUID;

import com.autovision.platform.tenant.AuthenticatedTenantContext;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional(readOnly = true)
public class DealerService {

    private final DealerRepository dealerRepository;

    public DealerService(DealerRepository dealerRepository) {
        this.dealerRepository = dealerRepository;
    }

    public List<DealerResponse> findAll(
            AuthenticatedTenantContext tenantContext
    ) {
        return dealerRepository
                .findAllByTenantId(tenantContext.tenantId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public DealerResponse findById(
            AuthenticatedTenantContext tenantContext,
            UUID dealerId
    ) {
        Dealer dealer = dealerRepository
                .findByIdAndTenantId(
                        dealerId,
                        tenantContext.tenantId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND,
                        "Dealer not found"
                ));

        return toResponse(dealer);
    }

    private DealerResponse toResponse(Dealer dealer) {
        return new DealerResponse(
                dealer.getId(),
                dealer.getCode(),
                dealer.getName(),
                dealer.getLegalName(),
                dealer.getPrimaryLocationId(),
                dealer.getStatus(),
                dealer.getCreatedAt(),
                dealer.getUpdatedAt()
        );
    }
}