package com.autovision.platform.tenant;

import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class TenantContextResolver {

    public static final String USER_REF_CLAIM = "autovision_user_ref_id";

    private final UserRefLookupRepository userRefLookupRepository;

    public TenantContextResolver(
            UserRefLookupRepository userRefLookupRepository
    ) {
        this.userRefLookupRepository = userRefLookupRepository;
    }

    public AuthenticatedTenantContext resolve(Jwt jwt) {
    String userRefClaim =
            jwt.getClaimAsString(USER_REF_CLAIM);

    if (userRefClaim == null || userRefClaim.isBlank()) {
        throw new AccessDeniedException(
                "Authenticated identity has no AutoVision user reference"
        );
    }

    UUID userRefId;

    try {
        userRefId = UUID.fromString(userRefClaim);
    }
    catch (IllegalArgumentException ex) {
        throw new AccessDeniedException(
                "Authenticated identity has an invalid AutoVision user reference"
        );
    }

    UserRefLookupRepository.UserRefRecord userRef =
            userRefLookupRepository
                    .findActiveById(userRefId)
                    .orElseThrow(() -> new AccessDeniedException(
                            "Authenticated identity is not mapped to an active AutoVision user"
                    ));

    return new AuthenticatedTenantContext(
            userRef.id(),
            userRef.tenantId(),
            userRef.externalUserId()
    );
}
}