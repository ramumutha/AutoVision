package com.autovision.platform.organization;

import java.lang.reflect.Field;
import java.math.BigDecimal;
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

class LocationServiceTests {

    private final LocationRepository repository =
            mock(LocationRepository.class);

    private final LocationService service =
            new LocationService(repository);

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
    void listsOnlyAuthenticatedTenantLocations()
            throws Exception {

        Location location = location(
                UUID.randomUUID(),
                tenantId,
                "L001",
                "Demo Location"
        );

        when(repository.findAllByTenantId(tenantId))
                .thenReturn(List.of(location));

        List<LocationResponse> result =
                service.findAll(context);

        assertEquals(1, result.size());
        assertEquals("L001", result.getFirst().code());

        verify(repository)
                .findAllByTenantId(tenantId);
    }

    @Test
    void findsLocationOnlyWithinAuthenticatedTenant()
            throws Exception {

        UUID locationId = UUID.randomUUID();

        Location location = location(
                locationId,
                tenantId,
                "L001",
                "Demo Location"
        );

        when(repository.findByIdAndTenantId(
                locationId,
                tenantId
        )).thenReturn(Optional.of(location));

        LocationResponse result =
                service.findById(context, locationId);

        assertEquals(locationId, result.id());

        verify(repository)
                .findByIdAndTenantId(
                        locationId,
                        tenantId
                );
    }

    @Test
    void returnsNotFoundForLocationOutsideTenant() {
        UUID locationId = UUID.randomUUID();

        when(repository.findByIdAndTenantId(
                locationId,
                tenantId
        )).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.findById(
                                context,
                                locationId
                        )
                );

        assertEquals(
                404,
                exception.getStatusCode().value()
        );
    }

    private Location location(
            UUID id,
            UUID tenantId,
            String code,
            String name
    ) throws Exception {

        Location location = new Location();

        set(location, "id", id);
        set(location, "tenantId", tenantId);
        set(location, "code", code);
        set(location, "name", name);
        set(location, "city", "Bengaluru");
        set(location, "countryCode", "IN");
        set(location, "timezone", "Asia/Kolkata");
        set(
                location,
                "latitude",
                new BigDecimal("12.971599")
        );
        set(
                location,
                "longitude",
                new BigDecimal("77.594566")
        );
        set(
                location,
                "status",
                OrganizationStatus.ACTIVE
        );
        set(
                location,
                "createdAt",
                OffsetDateTime.now()
        );
        set(
                location,
                "updatedAt",
                OffsetDateTime.now()
        );

        return location;
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