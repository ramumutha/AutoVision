package com.autovision.platform.serviceprofit;

import java.time.LocalDate;

public record ServiceProfitOpportunityContextSnapshot(
        String customerDisplayName,
        String customerReference,
        String customerPhone,
        String customerEmail,
        Boolean customerContactable,
        String vehicleRegistration,
        String vehicleVin,
        String vehicleMake,
        String vehicleModel,
        Integer vehicleModelYear,
        String vehiclePowertrain,
        String serviceOrderReference,
        LocalDate serviceDate,
        String serviceDescription,
        String serviceAdvisorContext
) {
}