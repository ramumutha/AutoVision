package com.autovision.platform.organization;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "dealers",
        schema = "platform",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_dealers_tenant_code",
                        columnNames = {"tenant_id", "code"}
                )
        }
)
public class Dealer {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 80)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "legal_name", length = 255)
    private String legalName;

    @Column(name = "primary_location_id")
    private UUID primaryLocationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrganizationStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Dealer() {
    }
}