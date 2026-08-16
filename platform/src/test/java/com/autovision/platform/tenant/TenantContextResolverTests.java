package com.autovision.platform.tenant;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantContextResolverTests {

    private final UserRefLookupRepository repository =
            mock(UserRefLookupRepository.class);

    private final TenantContextResolver resolver =
            new TenantContextResolver(repository);

    @Test
    void resolvesMappedActiveUser() {
        UUID userRefId =
                UUID.fromString(
                        "761b3ab6-bd03-48c0-a107-44fa1403b0f3"
                );

        UUID tenantId =
                UUID.fromString(
                        "2cf85fea-bc61-4405-be50-00a0ca45df3b"
                );

        when(repository.findActiveById(userRefId))
                .thenReturn(Optional.of(
                        new UserRefLookupRepository.UserRefRecord(
                                userRefId,
                                tenantId,
                                "svc-advisor-01"
                        )
                ));

        AuthenticatedTenantContext context =
                resolver.resolve(jwtWithUserRef(userRefId));

        assertEquals(userRefId, context.userRefId());
        assertEquals(tenantId, context.tenantId());
        assertEquals(
                "svc-advisor-01",
                context.externalUserId()
        );
    }

    @Test
    void rejectsJwtWithoutAutoVisionUserReference() {
        Jwt jwt = jwt(Map.of(
                "sub",
                "service-account-test"
        ));

        assertThrows(
                AccessDeniedException.class,
                () -> resolver.resolve(jwt)
        );
    }

    @Test
    void rejectsUnmappedOrInactiveUser() {
        UUID unknownUserRefId =
                UUID.fromString(
                        "00000000-0000-0000-0000-000000009999"
                );

        when(repository.findActiveById(unknownUserRefId))
                .thenReturn(Optional.empty());

        assertThrows(
                AccessDeniedException.class,
                () -> resolver.resolve(
                        jwtWithUserRef(unknownUserRefId)
                )
        );
    }

    @Test
    void rejectsMalformedUserReference() {
        Jwt jwt = jwt(Map.of(
                TenantContextResolver.USER_REF_CLAIM,
                "not-a-uuid"
        ));

        assertThrows(
                AccessDeniedException.class,
                () -> resolver.resolve(jwt)
        );
    }

    private Jwt jwtWithUserRef(UUID userRefId) {
        return jwt(Map.of(
                TenantContextResolver.USER_REF_CLAIM,
                userRefId.toString()
        ));
    }

    private Jwt jwt(Map<String, Object> claims) {
        return new Jwt(
                "test-token",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "none"),
                claims
        );
    }
}