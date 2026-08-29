package com.autovision.platform.commercial;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Profile("local | test")
public class LocalCommercialVerificationDelivery implements CommercialVerificationDelivery {
    private final AtomicReference<String> latestLink = new AtomicReference<>();

    @Override
    public void deliver(UUID enquiryId, String email, String token) {
        latestLink.set("/contact/verify?token=" + token);
    }

    public String latestLink() { return latestLink.get(); }
}
