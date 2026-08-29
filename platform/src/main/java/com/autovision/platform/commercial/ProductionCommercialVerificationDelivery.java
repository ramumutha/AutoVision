package com.autovision.platform.commercial;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("production")
public class ProductionCommercialVerificationDelivery implements CommercialVerificationDelivery {
    @Override
    public void deliver(UUID enquiryId, String email, String token) {
        throw new IllegalStateException("Commercial verification delivery is not configured");
    }
}
