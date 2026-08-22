package com.autovision.platform.serviceprofit;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceProfitOpportunityQueryTests {

    @Test
    void defaultsNullSortToDetectedDescending() {

        ServiceProfitOpportunityQuery query =
                new ServiceProfitOpportunityQuery(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        0,
                        25,
                        null
                );

        assertEquals(
                ServiceProfitOpportunitySort.DETECTED_DESC,
                query.sort()
        );

        assertEquals(0, query.offset());
    }

    @Test
    void calculatesPageOffset() {

        ServiceProfitOpportunityQuery query =
                new ServiceProfitOpportunityQuery(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        3,
                        25,
                        ServiceProfitOpportunitySort.DETECTED_DESC
                );

        assertEquals(75, query.offset());
    }

    @Test
    void rejectsNegativePage() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new ServiceProfitOpportunityQuery(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        -1,
                        25,
                        ServiceProfitOpportunitySort.DETECTED_DESC
                )
        );
    }

    @Test
    void rejectsSizeBelowOne() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new ServiceProfitOpportunityQuery(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        0,
                        0,
                        ServiceProfitOpportunitySort.DETECTED_DESC
                )
        );
    }

    @Test
    void rejectsSizeAboveOneHundred() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new ServiceProfitOpportunityQuery(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        0,
                        101,
                        ServiceProfitOpportunitySort.DETECTED_DESC
                )
        );
    }

    @Test
    void pageCalculatesTotalPages() {

        ServiceProfitOpportunityPage page =
                ServiceProfitOpportunityPage.of(
                        List.of(),
                        0,
                        25,
                        51
                );

        assertEquals(3, page.totalPages());
        assertEquals(51, page.totalElements());
    }
}
