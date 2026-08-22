package com.autovision.platform.serviceprofit.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "dealer_data_capability_assessments",
        schema = "platform",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_dealer_data_capability_assessment",
                        columnNames = {
                                "assessment_id",
                                "capability"
                        }
                )
        }
)
public class DealerDataCapabilityAssessment {

    @Id
    private UUID id;

    @Column(name = "assessment_id", nullable = false)
    private UUID assessmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "capability", nullable = false, length = 60)
    private DealerDataCapability capability;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DealerDataCapabilityStatus status;

    @Column(name = "reason", nullable = false, length = 1000)
    private String reason;

    @Column(name = "created_by_principal_id", nullable = false)
    private UUID createdByPrincipalId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected DealerDataCapabilityAssessment() {
    }

    public static DealerDataCapabilityAssessment create(
            UUID id,
            UUID assessmentId,
            DealerDataCapabilityResult result,
            UUID principalId,
            OffsetDateTime now
    ) {
        requireId(
                id,
                "Dealer data capability assessment ID is required"
        );

        requireId(
                assessmentId,
                "Dealer data assessment ID is required"
        );

        if (result == null) {
            throw new IllegalArgumentException(
                    "Dealer data capability result is required"
            );
        }

        requireId(principalId, "Principal ID is required");
        requireTime(now, "Capability creation time is required");

        DealerDataCapabilityAssessment assessment =
                new DealerDataCapabilityAssessment();

        assessment.id = id;
        assessment.assessmentId = assessmentId;
        assessment.capability = result.capability();
        assessment.status = result.status();
        assessment.reason = result.reason();
        assessment.createdByPrincipalId = principalId;
        assessment.createdAt = now;

        return assessment;
    }

    private static void requireId(
            UUID value,
            String message
    ) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireTime(
            OffsetDateTime value,
            String message
    ) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    public UUID getId() { return id; }
    public UUID getAssessmentId() { return assessmentId; }
    public DealerDataCapability getCapability() { return capability; }
    public DealerDataCapabilityStatus getStatus() { return status; }
    public String getReason() { return reason; }
    public UUID getCreatedByPrincipalId() { return createdByPrincipalId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}

