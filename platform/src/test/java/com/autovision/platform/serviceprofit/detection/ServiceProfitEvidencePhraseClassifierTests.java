package com.autovision.platform.serviceprofit.detection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServiceProfitEvidencePhraseClassifierTests {

    private final ServiceProfitEvidencePhraseClassifier classifier =
            new ServiceProfitEvidencePhraseClassifier();

    @Test
    void classifiesStrongDecline() {
        assertEquals(
                ServiceProfitEvidenceSignal.STRONG_DECLINE,
                classifier.classify(
                        "Customer declined front brake work today."
                )
        );
    }

    @Test
    void classifiesStrongDeferredEvidence() {
        assertEquals(
                ServiceProfitEvidenceSignal.STRONG_DEFER,
                classifier.classify(
                        "Customer advised and will do next visit."
                )
        );
    }

    @Test
    void classifiesAmbiguousEvidence() {
        assertEquals(
                ServiceProfitEvidenceSignal.AMBIGUOUS_POSTPONEMENT,
                classifier.classify(
                        "Replacement recommended to customer."
                )
        );
    }

    @Test
    void completionTakesPrecedence() {
        assertEquals(
                ServiceProfitEvidenceSignal.COMPLETION,
                classifier.classify(
                        "Previously discussed with customer. Work completed today."
                )
        );
    }

    @Test
    void unrelatedTextProducesNoSignal() {
        assertEquals(
                ServiceProfitEvidenceSignal.NONE,
                classifier.classify(
                        "Vehicle washed and ready for collection."
                )
        );
    }
}
