package com.autovision.platform.organization;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "locations",
        schema = "platform",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_locations_tenant_code",
                        columnNames = {"tenant_id", "code"}
                )
        }
)
public class Location {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 80)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "address_line_1", length = 255)
    private String addressLine1;

    @Column(name = "address_line_2", length = 255)
    private String addressLine2;

    @Column(length = 120)
    private String city;

    @Column(name = "state_province", length = 120)
    private String stateProvince;

    @Column(name = "postal_code", length = 40)
    private String postalCode;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(length = 80)
    private String timezone;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrganizationStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Location() {
    }
}