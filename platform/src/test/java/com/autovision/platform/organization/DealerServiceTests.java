package com.autovision.platform.organization;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.autovision.platform.tenant.AuthenticatedTenantContext;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DealerServiceTests {

    private final DealerRepository repository =
            mock(DealerRepository.class);

    private final DealerService service =
            new DealerService(repository);

    private final UUID tenantId =
            UUID.fromString(
                    "2cf85fea-bc61-4405-be50-00a0ca45df3b"
            );

    private final AuthenticatedTenantContext context =
            new AuthenticatedTenantContext(
                    UUID.fromString(
                            "761b3ab6-bd03-48c0-a107-44fa1403b0f3"
                    ),
                    tenantId,
                    "svc-advisor-01"
            );

    @Test
    void listsOnlyAuthenticatedTenantDealers() throws Exception {
        Dealer dealer = dealer(
                UUID.randomUUID(),
                tenantId,
                "D001",
                "Demo Dealer"
        );

        when(repository.findAllByTenantId(tenantId))
                .thenReturn(List.of(dealer));

        List<DealerResponse> result =
                service.findAll(context);

        assertEquals(1, result.size());
        assertEquals("D001", result.getFirst().code());

        verify(repository)
                .findAllByTenantId(tenantId);
    }

    @Test
    void findsDealerOnlyWithinAuthenticatedTenant()
            throws Exception {

        UUID dealerId = UUID.randomUUID();

        Dealer dealer = dealer(
                dealerId,
                tenantId,
                "D001",
                "Demo Dealer"
        );

        when(repository.findByIdAndTenantId(
                dealerId,
                tenantId
        )).thenReturn(Optional.of(dealer));

        DealerResponse result =
                service.findById(context, dealerId);

        assertEquals(dealerId, result.id());

        verify(repository)
                .findByIdAndTenantId(
                        dealerId,
                        tenantId
                );
    }

    @Test
    void returnsNotFoundForDealerOutsideTenant() {
        UUID dealerId = UUID.randomUUID();

        when(repository.findByIdAndTenantId(
                dealerId,
                tenantId
        )).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.findById(
                                context,
                                dealerId
                        )
                );

        assertEquals(
                404,
                exception.getStatusCode().value()
        );
    }

    private Dealer dealer(
            UUID id,
            UUID tenantId,
            String code,
            String name
    ) throws Exception {

        Dealer dealer = new Dealer();

        set(dealer, "id", id);
        set(dealer, "tenantId", tenantId);
        set(dealer, "code", code);
        set(dealer, "name", name);
        set(
                dealer,
                "status",
                OrganizationStatus.ACTIVE
        );
        set(
                dealer,
                "createdAt",
                OffsetDateTime.now()
        );
        set(
                dealer,
                "updatedAt",
                OffsetDateTime.now()
        );

        return dealer;
    }

    private void set(
            Object target,
            String fieldName,
            Object value
    ) throws Exception {

        Field field =
                target.getClass()
                        .getDeclaredField(fieldName);

        field.setAccessible(true);
        field.set(target, value);
    }
}