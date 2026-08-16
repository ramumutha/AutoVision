package com.autovision.platform.tenant;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class UserRefLookupRepository {

    private final JdbcClient jdbcClient;

    public UserRefLookupRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<UserRefRecord> findActiveById(UUID userRefId) {
    return jdbcClient.sql("""
            SELECT id, tenant_id, external_user_id
              FROM public.user_refs
             WHERE id = :userRefId
               AND is_active = TRUE
            """)
            .param("userRefId", userRefId)
            .query((rs, rowNum) -> new UserRefRecord(
                    rs.getObject("id", UUID.class),
                    rs.getObject("tenant_id", UUID.class),
                    rs.getString("external_user_id")
            ))
            .optional();
}

    public record UserRefRecord(
            UUID id,
            UUID tenantId,
            String externalUserId
    ) {
    }
}