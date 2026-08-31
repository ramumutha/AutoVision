package com.autovision.platform.commercial;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = false)
public record CommercialEnquiryRequest(
        @NotNull CommercialEnquiryPurpose purpose,
        @NotBlank @Size(max = 160) String companyName,
        @NotBlank @Size(max = 80) String firstName,
        @NotBlank @Size(max = 80) String lastName,
        @NotBlank @Email @Size(max = 254) String businessEmail,
        @NotBlank @Size(max = 120) String roleOrTitle,
        @NotBlank @Size(max = 80) String countryOrMarket,
        @Size(max = 2000) String messageOrRequirement,
        CommercialProductFamily productFamily,
        CommercialProduct product,
        AdvisoryArea advisoryArea,
        @Size(max = 8) List<String> products,
        @Size(max = 8) List<String> servicePractices,
        @Size(max = 8) List<String> supportTypes,
        String evaluationPreference,
        String organizationType,
        String partnershipType,
        String projectStage,
        String desiredTimeframe,
        String currentTechnology,
        @Size(max = 8) List<String> businessObjectives,
        Integer serviceLocations,
        String monthlyServiceOrders,
        String historicalDataAvailability,
        Boolean declinedRecommendationsRecorded,
        String phone,
        String preferredContactMethod,
        String cityRegion,
        String companyWebsite
) {
    public CommercialEnquiryRequest(CommercialEnquiryPurpose purpose, String companyName, String firstName,
            String lastName, String businessEmail, String roleOrTitle, String countryOrMarket,
            String messageOrRequirement, CommercialProductFamily productFamily, CommercialProduct product,
            AdvisoryArea advisoryArea) {
            this(purpose, companyName, firstName, lastName, businessEmail, roleOrTitle, countryOrMarket,
                    messageOrRequirement, productFamily, product, advisoryArea, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
