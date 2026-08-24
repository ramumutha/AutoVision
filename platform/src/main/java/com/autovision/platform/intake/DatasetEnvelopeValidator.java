package com.autovision.platform.intake;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class DatasetEnvelopeValidator {

    public static final String CONTRACT_ID = "AV-R1-CDF-001";
    public static final String CONTRACT_VERSION = "1.0";

    public List<IntakeFinding> validate(ControlledDatasetEnvelope envelope) {
        List<IntakeFinding> findings = new ArrayList<>();
        if (!CONTRACT_ID.equals(envelope.contractId())
                || !CONTRACT_VERSION.equals(envelope.contractVersion())) {
            findings.add(fatal(IntakeValidationCode.INTAKE_UNSUPPORTED_CONTRACT_VERSION,
                    "contractVersion", "The controlled contract version is unsupported"));
        }
        if (!"FULL".equals(envelope.deliveryType())) {
            findings.add(fatal(IntakeValidationCode.INTAKE_UNSUPPORTED_DELIVERY_TYPE,
                    "deliveryType", "Only FULL dataset delivery is supported"));
        }
        if (envelope.effectiveFrom() != null && envelope.effectiveTo() != null
                && envelope.effectiveTo().isBefore(envelope.effectiveFrom())) {
            findings.add(fatal(IntakeValidationCode.INTAKE_INVALID_DATE,
                    "effectiveTo", "The effective date window is invalid"));
        }
        return List.copyOf(findings);
    }

    private IntakeFinding fatal(IntakeValidationCode code, String path, String message) {
        return new IntakeFinding(ValidationStage.ENVELOPE, ValidationSeverity.FATAL,
                code, path, message, null);
    }
}