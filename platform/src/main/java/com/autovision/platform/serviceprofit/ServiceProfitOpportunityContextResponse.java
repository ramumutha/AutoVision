package com.autovision.platform.serviceprofit;

import java.time.LocalDate;

public record ServiceProfitOpportunityContextResponse(
        Customer customer,
        Vehicle vehicle,
        Service service
) {

    public static ServiceProfitOpportunityContextResponse from(
            ServiceProfitOpportunityContext context
    ) {
        if (context == null) {
            return null;
        }

        return new ServiceProfitOpportunityContextResponse(
                new Customer(
                        context.getCustomerDisplayName(),
                        context.getCustomerReference(),
                        context.getCustomerPhone(),
                        context.getCustomerEmail(),
                        context.getCustomerContactable()
                ),
                new Vehicle(
                        context.getVehicleRegistration(),
                        context.getVehicleVin(),
                        context.getVehicleMake(),
                        context.getVehicleModel(),
                        context.getVehicleModelYear(),
                        context.getVehiclePowertrain()
                ),
                new Service(
                        context.getServiceOrderReference(),
                        context.getServiceDate(),
                        context.getServiceDescription(),
                        context.getServiceAdvisorContext()
                )
        );
    }

    public record Customer(
            String displayName,
            String reference,
            String phone,
            String email,
            Boolean contactable
    ) {
    }

    public record Vehicle(
            String registration,
            String vin,
            String make,
            String model,
            Integer modelYear,
            String powertrain
    ) {
    }

    public record Service(
            String orderReference,
            LocalDate serviceDate,
            String description,
            String advisorContext
    ) {
    }
}