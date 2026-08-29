package com.autovision.platform.commercial;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalCommercialVerificationDeliveryTests {
    @Test
    void capturesOnlyTheMostRecentLinkInMemory() {
        LocalCommercialVerificationDelivery delivery = new LocalCommercialVerificationDelivery();
        delivery.deliver(UUID.randomUUID(), "synthetic@example.invalid", "first-token");
        delivery.deliver(UUID.randomUUID(), "synthetic@example.invalid", "second-token");

        assertEquals("/contact/verify?token=second-token", delivery.latestLink());
    }

    @Test
    void localDeliveryBeanIsUnavailableUnderProductionProfile() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("production");
            context.register(LocalCommercialVerificationDelivery.class);
            context.refresh();

            assertThrows(NoSuchBeanDefinitionException.class,
                    () -> context.getBean(LocalCommercialVerificationDelivery.class));
        }
    }
}