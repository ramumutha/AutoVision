package com.autovision.platform.serviceprofit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "service_profit_opportunity_contexts",
        schema = "platform",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_service_profit_opportunity_contexts_opportunity",
                        columnNames = "opportunity_id"
                ),
                @UniqueConstraint(
                        name = "uq_service_profit_opportunity_contexts_id_tenant",
                        columnNames = {"id", "tenant_id"}
                )
        }
)
public class ServiceProfitOpportunityContext {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

        @Column(name = "opportunity_id", nullable = false)
        private UUID opportunityId;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumns({
            @JoinColumn(
                name = "opportunity_id",
                referencedColumnName = "id",
                nullable = false,
                insertable = false,
                updatable = false
            ),
            @JoinColumn(
                name = "tenant_id",
                referencedColumnName = "tenant_id",
                nullable = false,
                insertable = false,
                updatable = false
            )
        })
        private ServiceProfitOpportunity opportunity;

    @Column(name = "customer_display_name", length = 200)
    private String customerDisplayName;

    @Column(name = "customer_reference", length = 120)
    private String customerReference;

    @Column(name = "customer_phone", length = 80)
    private String customerPhone;

    @Column(name = "customer_email", length = 320)
    private String customerEmail;

    @Column(name = "customer_contactable")
    private Boolean customerContactable;

    @Column(name = "vehicle_registration", length = 80)
    private String vehicleRegistration;

    @Column(name = "vehicle_vin", length = 80)
    private String vehicleVin;

    @Column(name = "vehicle_make", length = 120)
    private String vehicleMake;

    @Column(name = "vehicle_model", length = 120)
    private String vehicleModel;

    @Column(name = "vehicle_model_year")
    private Integer vehicleModelYear;

    @Column(name = "vehicle_powertrain", length = 80)
    private String vehiclePowertrain;

    @Column(name = "service_order_reference", length = 120)
    private String serviceOrderReference;

    @Column(name = "service_date")
    private LocalDate serviceDate;

    @Column(name = "service_description", length = 1000)
    private String serviceDescription;

    @Column(name = "service_advisor_context", length = 1000)
    private String serviceAdvisorContext;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ServiceProfitOpportunityContext() {
    }

    public static ServiceProfitOpportunityContext capture(
            UUID id,
            UUID tenantId,
            ServiceProfitOpportunity opportunity,
            ServiceProfitOpportunityContextSnapshot snapshot,
            OffsetDateTime capturedAt
    ) {
        if (id == null || tenantId == null || opportunity == null) {
            throw new IllegalArgumentException(
                    "Context ID, tenant ID and opportunity ID are required"
            );
        }
        if (!tenantId.equals(opportunity.getTenantId())) {
            throw new IllegalArgumentException(
                    "Context tenant must match opportunity tenant"
            );
        }
        if (snapshot == null || capturedAt == null) {
            throw new IllegalArgumentException(
                    "Context snapshot and capture time are required"
            );
        }

        ServiceProfitOpportunityContext context =
                new ServiceProfitOpportunityContext();
        context.id = id;
        context.tenantId = tenantId;
        context.opportunityId = opportunity.getId();
        context.opportunity = opportunity;
        context.createdAt = capturedAt;
        context.apply(snapshot, capturedAt);
        return context;
    }

    public void refresh(
            ServiceProfitOpportunityContextSnapshot snapshot,
            OffsetDateTime refreshedAt
    ) {
        if (snapshot == null || refreshedAt == null) {
            throw new IllegalArgumentException(
                    "Context snapshot and refresh time are required"
            );
        }
        apply(snapshot, refreshedAt);
    }

    private void apply(
            ServiceProfitOpportunityContextSnapshot snapshot,
            OffsetDateTime changedAt
    ) {
        customerDisplayName = normalize(snapshot.customerDisplayName());
        customerReference = normalize(snapshot.customerReference());
        customerPhone = normalize(snapshot.customerPhone());
        customerEmail = normalize(snapshot.customerEmail());
        customerContactable = snapshot.customerContactable();
        vehicleRegistration = normalize(snapshot.vehicleRegistration());
        vehicleVin = normalize(snapshot.vehicleVin());
        vehicleMake = normalize(snapshot.vehicleMake());
        vehicleModel = normalize(snapshot.vehicleModel());
        vehicleModelYear = snapshot.vehicleModelYear();
        vehiclePowertrain = normalize(snapshot.vehiclePowertrain());
        serviceOrderReference = normalize(snapshot.serviceOrderReference());
        serviceDate = snapshot.serviceDate();
        serviceDescription = normalize(snapshot.serviceDescription());
        serviceAdvisorContext = normalize(snapshot.serviceAdvisorContext());
        updatedAt = changedAt;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getOpportunityId() {
        return opportunityId;
    }

    public String getCustomerDisplayName() {
        return customerDisplayName;
    }

    public String getCustomerReference() {
        return customerReference;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public Boolean getCustomerContactable() {
        return customerContactable;
    }

    public String getVehicleRegistration() {
        return vehicleRegistration;
    }

    public String getVehicleVin() {
        return vehicleVin;
    }

    public String getVehicleMake() {
        return vehicleMake;
    }

    public String getVehicleModel() {
        return vehicleModel;
    }

    public Integer getVehicleModelYear() {
        return vehicleModelYear;
    }

    public String getVehiclePowertrain() {
        return vehiclePowertrain;
    }

    public String getServiceOrderReference() {
        return serviceOrderReference;
    }

    public LocalDate getServiceDate() {
        return serviceDate;
    }

    public String getServiceDescription() {
        return serviceDescription;
    }

    public String getServiceAdvisorContext() {
        return serviceAdvisorContext;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}