package com.autovision.platform.commercial;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "commercial_enquiries", schema = "platform")
public class CommercialEnquiry {
    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CommercialEnquiryPurpose purpose;

    @Column(name = "company_name", nullable = false, length = 160)
    private String companyName;
    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;
    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;
    @Column(name = "business_email", nullable = false, length = 254)
    private String businessEmail;
    @Enumerated(EnumType.STRING)
    @Column(name = "email_classification", nullable = false, length = 24)
    private CommercialEmailClassification emailClassification;
    @Column(name = "role_or_title", nullable = false, length = 120)
    private String roleOrTitle;
    @Column(name = "country_or_market", nullable = false, length = 80)
    private String countryOrMarket;
    @Column(name = "message_or_requirement", nullable = false, length = 2000)
    private String messageOrRequirement;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_family", length = 32)
    private CommercialProductFamily productFamily;
    @Enumerated(EnumType.STRING)
    @Column(name = "product", length = 40)
    private CommercialProduct product;
    @Enumerated(EnumType.STRING)
    @Column(name = "advisory_area", length = 48)
    private AdvisoryArea advisoryArea;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "qualification", columnDefinition = "jsonb")
    private Map<String, Object> qualification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CommercialEnquiryStatus status;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @Version
    @Column(nullable = false)
    private long version;

    protected CommercialEnquiry() { }

    public static CommercialEnquiry submit(UUID id, CommercialEnquiryPurpose purpose, String companyName,
            String firstName, String lastName, String businessEmail,
            CommercialEmailClassification emailClassification, String roleOrTitle,
            String countryOrMarket, String messageOrRequirement,
            CommercialProductFamily productFamily, CommercialProduct product,
            AdvisoryArea advisoryArea, Map<String, Object> qualification, OffsetDateTime now) {
        CommercialEnquiry enquiry = new CommercialEnquiry();
        enquiry.id = id;
        enquiry.purpose = purpose;
        enquiry.companyName = companyName;
        enquiry.firstName = firstName;
        enquiry.lastName = lastName;
        enquiry.businessEmail = businessEmail;
        enquiry.emailClassification = emailClassification;
        enquiry.roleOrTitle = roleOrTitle;
        enquiry.countryOrMarket = countryOrMarket;
        enquiry.messageOrRequirement = messageOrRequirement;
        enquiry.productFamily = productFamily;
        enquiry.product = product;
        enquiry.advisoryArea = advisoryArea;
        enquiry.qualification = qualification;
        enquiry.status = CommercialEnquiryStatus.VERIFICATION_PENDING;
        enquiry.createdAt = now;
        enquiry.updatedAt = now;
        return enquiry;
    }

    public static CommercialEnquiry submit(UUID id, CommercialEnquiryPurpose purpose, String companyName,
            String firstName, String lastName, String businessEmail,
            CommercialEmailClassification emailClassification, String roleOrTitle,
            String countryOrMarket, String messageOrRequirement,
            CommercialProductFamily productFamily, CommercialProduct product,
            AdvisoryArea advisoryArea, OffsetDateTime now) {
        return submit(id, purpose, companyName, firstName, lastName, businessEmail, emailClassification,
                roleOrTitle, countryOrMarket, messageOrRequirement, productFamily, product, advisoryArea, null, now);
    }

    public void verify(OffsetDateTime now) {
        if (status != CommercialEnquiryStatus.VERIFICATION_PENDING) throw new IllegalStateException("Enquiry is not awaiting verification");
        status = CommercialEnquiryStatus.VERIFIED;
        updatedAt = now;
    }

    public UUID getId() { return id; }
    public CommercialEnquiryPurpose getPurpose() { return purpose; }
    public CommercialEnquiryStatus getStatus() { return status; }
    public String getBusinessEmail() { return businessEmail; }
    public CommercialEmailClassification getEmailClassification() { return emailClassification; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
