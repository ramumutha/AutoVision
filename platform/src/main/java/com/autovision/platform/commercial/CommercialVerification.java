package com.autovision.platform.commercial;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "commercial_verifications", schema = "platform")
public class CommercialVerification {
    @Id
    private UUID id;
    @Column(name = "enquiry_id", nullable = false, unique = true)
    private UUID enquiryId;
    @Column(name = "token_digest", nullable = false, unique = true, length = 64)
    private String tokenDigest;
    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;
    @Column(name = "used_at")
    private OffsetDateTime usedAt;
    @Version
    @Column(nullable = false)
    private long version;

    protected CommercialVerification() { }

    public static CommercialVerification issue(UUID id, UUID enquiryId, String tokenDigest, OffsetDateTime expiresAt) {
        CommercialVerification verification = new CommercialVerification();
        verification.id = id;
        verification.enquiryId = enquiryId;
        verification.tokenDigest = tokenDigest;
        verification.expiresAt = expiresAt;
        return verification;
    }

    public boolean usableAt(OffsetDateTime now) { return usedAt == null && expiresAt.isAfter(now); }
    public void consume(OffsetDateTime now) { usedAt = now; }
    public UUID getEnquiryId() { return enquiryId; }
    public String getTokenDigest() { return tokenDigest; }
}
