package com.autovision.platform.commercial;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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
        AdvisoryArea advisoryArea
) { }
