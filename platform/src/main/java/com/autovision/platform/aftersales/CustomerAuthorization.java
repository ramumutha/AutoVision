package com.autovision.platform.aftersales;

import tools.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "customer_authorizations",
        schema = "platform",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_customer_authorizations_tenant_number",
                        columnNames = {"tenant_id", "authorization_number"}
                ),
                @UniqueConstraint(
                        name = "uq_customer_authorizations_id_tenant",
                        columnNames = {"id", "tenant_id"}
                )
        }
)
public class CustomerAuthorization {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "dealer_id")
    private UUID dealerId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "aftersales_case_id", nullable = false)
    private UUID aftersalesCaseId;

    @Column(name = "authorization_number", nullable = false, length = 80)
    private String authorizationNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "authorization_status", nullable = false, length = 32)
    private CustomerAuthorizationStatus authorizationStatus;

    @Column(name = "customer_reference", length = 160)
    private String customerReference;

    @Column(name = "customer_display_name_snapshot", length = 200)
    private String customerDisplayNameSnapshot;

    @Column(name = "authorization_summary", nullable = false, length = 500)
    private String authorizationSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "authorization_scope_snapshot", nullable = false)
    private JsonNode authorizationScopeSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "commercial_snapshot")
    private JsonNode commercialSnapshot;

    @Column(name = "terms_snapshot")
    private String termsSnapshot;

    @Column(name = "disclaimer_snapshot")
    private String disclaimerSnapshot;

    @Column(name = "requested_at", nullable = false)
    private OffsetDateTime requestedAt;

    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    @Column(name = "decision_channel", length = 32)
    private String decisionChannel;

    @Column(name = "decision_reference", length = 160)
    private String decisionReference;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_by_principal_id")
    private UUID createdByPrincipalId;

    @Column(name = "updated_by_principal_id")
    private UUID updatedByPrincipalId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected CustomerAuthorization() {
    }

    public static CustomerAuthorization request(
            UUID id,
            UUID tenantId,
            UUID dealerId,
            UUID branchId,
            UUID aftersalesCaseId,
            String authorizationNumber,
            String customerReference,
            String customerDisplayNameSnapshot,
            String authorizationSummary,
            JsonNode authorizationScopeSnapshot,
            JsonNode commercialSnapshot,
            String termsSnapshot,
            String disclaimerSnapshot,
            UUID principalId,
            OffsetDateTime now
    ) {
        CustomerAuthorization authorization = new CustomerAuthorization();

        authorization.id = id;
        authorization.tenantId = tenantId;
        authorization.dealerId = dealerId;
        authorization.branchId = branchId;
        authorization.aftersalesCaseId = aftersalesCaseId;
        authorization.authorizationNumber = authorizationNumber;
        authorization.authorizationStatus =
                CustomerAuthorizationStatus.REQUESTED;
        authorization.customerReference = customerReference;
        authorization.customerDisplayNameSnapshot =
                customerDisplayNameSnapshot;
        authorization.authorizationSummary = authorizationSummary;
        authorization.authorizationScopeSnapshot =
                authorizationScopeSnapshot;
        authorization.commercialSnapshot = commercialSnapshot;
        authorization.termsSnapshot = termsSnapshot;
        authorization.disclaimerSnapshot = disclaimerSnapshot;
        authorization.requestedAt = now;
        authorization.createdByPrincipalId = principalId;
        authorization.updatedByPrincipalId = principalId;
        authorization.createdAt = now;
        authorization.updatedAt = now;

        return authorization;
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

    public UUID getBranchId() {
        return branchId;
    }

    public UUID getAftersalesCaseId() {
        return aftersalesCaseId;
    }

    public String getAuthorizationNumber() {
        return authorizationNumber;
    }

    public CustomerAuthorizationStatus getAuthorizationStatus() {
        return authorizationStatus;
    }

    public String getCustomerReference() {
        return customerReference;
    }

    public String getCustomerDisplayNameSnapshot() {
        return customerDisplayNameSnapshot;
    }

    public String getAuthorizationSummary() {
        return authorizationSummary;
    }

    public JsonNode getAuthorizationScopeSnapshot() {
        return authorizationScopeSnapshot;
    }

    public JsonNode getCommercialSnapshot() {
        return commercialSnapshot;
    }

    public String getTermsSnapshot() {
        return termsSnapshot;
    }

    public String getDisclaimerSnapshot() {
        return disclaimerSnapshot;
    }

    public OffsetDateTime getRequestedAt() {
        return requestedAt;
    }

    public OffsetDateTime getDecidedAt() {
        return decidedAt;
    }

    public String getDecisionChannel() {
        return decisionChannel;
    }

    public String getDecisionReference() {
        return decisionReference;
    }

    public long getVersion() {
        return version;
    }

    public UUID getCreatedByPrincipalId() {
        return createdByPrincipalId;
    }

    public UUID getUpdatedByPrincipalId() {
        return updatedByPrincipalId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
