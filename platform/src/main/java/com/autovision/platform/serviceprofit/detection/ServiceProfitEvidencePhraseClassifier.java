package com.autovision.platform.serviceprofit.detection;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.List;

@Component
public class ServiceProfitEvidencePhraseClassifier {

    private static final List<String> STRONG_DECLINE_PHRASES =
            List.of(
                    "customer declined",
                    "customer rejected",
                    "declined today",
                    "did not approve",
                    "not approved",
                    "customer refused"
            );

    private static final List<String> STRONG_DEFER_PHRASES =
            List.of(
                    "will do next visit",
                    "next visit",
                    "next month",
                    "defer",
                    "deferred",
                    "postponed",
                    "do later",
                    "will return"
            );

    private static final List<String> AMBIGUOUS_PHRASES =
            List.of(
                    "customer advised",
                    "discussed with customer",
                    "recommended to customer",
                    "monitor",
                    "consider replacement"
            );

    private static final List<String> AMBIGUOUS_IDENTITY_PHRASES =
            List.of(
                    "could not be confidently matched",
                    "customer could not be matched",
                    "unable to match customer",
                    "customer identity uncertain",
                    "ambiguous customer"
            );


    private static final List<String> COMPLETION_PHRASES =
            List.of(
                    "completed",
                    "work carried out",
                    "replaced today",
                    "repair completed",
                    "service completed"
            );

    public ServiceProfitEvidenceSignal classify(
            String text
    ) {
        if (text == null || text.isBlank()) {
            return ServiceProfitEvidenceSignal.NONE;
        }

        String normalized =
                text.toLowerCase(Locale.ROOT);

        if (containsAny(
                normalized,
                COMPLETION_PHRASES
        )) {
            return ServiceProfitEvidenceSignal.COMPLETION;
        }

        if (containsAny(
                normalized,
                STRONG_DECLINE_PHRASES
        )) {
            return ServiceProfitEvidenceSignal.STRONG_DECLINE;
        }

        if (containsAny(
                normalized,
                STRONG_DEFER_PHRASES
        )) {
            return ServiceProfitEvidenceSignal.STRONG_DEFER;
        }

        if (containsAny(
                normalized,
                AMBIGUOUS_IDENTITY_PHRASES
        )) {
            return ServiceProfitEvidenceSignal.AMBIGUOUS_IDENTITY;
        }


        if (containsAny(
                normalized,
                AMBIGUOUS_PHRASES
        )) {
            return ServiceProfitEvidenceSignal.AMBIGUOUS_POSTPONEMENT;
        }

        return ServiceProfitEvidenceSignal.NONE;
    }

    private boolean containsAny(
            String normalized,
            List<String> phrases
    ) {
        return phrases.stream()
                .anyMatch(normalized::contains);
    }
}
