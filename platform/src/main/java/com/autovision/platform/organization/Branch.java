package com.autovision.platform.organization;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "branches",
        schema = "platform",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_branches_tenant_dealer_code",
                        columnNames = {"tenant_id", "dealer_id", "code"}
                )
        }
)
public class Branch {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "dealer_id", nullable = false)
    private UUID dealerId;

    @Column(name = "location_id")
    private UUID locationId;

    @Column(nullable = false, length = 80)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrganizationStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Branch() {
    }

    public UUID getId() {
    return id;
}

public UUID getTenantId() {
    return tenantId;
}

public UUID getDealerId() {
    return dealerId;
}

public UUID getLocationId() {
    return locationId;
}

public String getCode() {
    return code;
}

public String getName() {
    return name;
}

public OrganizationStatus getStatus() {
    return status;
}

public OffsetDateTime getCreatedAt() {
    return createdAt;
}

public OffsetDateTime getUpdatedAt() {
    return updatedAt;
}
}