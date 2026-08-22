package com.autovision.platform.serviceprofit.demo;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class ServiceProfitDemoTenantRepository {

    private final JdbcClient jdbcClient;

    public ServiceProfitDemoTenantRepository(
            JdbcClient jdbcClient
    ) {
        this.jdbcClient = jdbcClient;
    }

    public boolean existsById(
            UUID tenantId
    ) {
        Integer count = jdbcClient.sql("""
                SELECT COUNT(*)
                  FROM public.tenants
                 WHERE id = :tenantId
                """)
                .param("tenantId", tenantId)
                .query(Integer.class)
                .single();

        return count != null && count > 0;
    }
}
