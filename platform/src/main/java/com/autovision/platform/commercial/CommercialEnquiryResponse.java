package com.autovision.platform.commercial;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CommercialEnquiryResponse(UUID id, CommercialEnquiryStatus status, OffsetDateTime createdAt) { }
