package com.autovision.platform.commercial;

import java.util.UUID;

public interface CommercialVerificationDelivery {
    void deliver(UUID enquiryId, String email, String token);
}
