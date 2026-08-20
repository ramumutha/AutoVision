package com.autovision.platform.aftersales;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ServiceQuoteRepositoryTests {

    @Autowired
    private ServiceQuoteRepository quoteRepository;

    @Autowired
    private ServiceOrderRepository orderRepository;

    @Autowired
    private AfterSalesCaseRepository caseRepository;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void createSchema() {
        exec("DROP TABLE IF EXISTS platform.service_quote_lines");
        exec("DROP TABLE IF EXISTS platform.service_quotes");
        exec("""
                CREATE TABLE platform.service_quotes (
                    id UUID PRIMARY KEY,
                    tenant_id UUID NOT NULL,
                    dealer_id UUID,
                    branch_id UUID,
                    after_sales_case_id UUID NOT NULL,
                    service_order_id UUID NOT NULL,
                    quote_number VARCHAR(80) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    currency_code VARCHAR(3) NOT NULL,
                    valid_until TIMESTAMP WITH TIME ZONE,
                    terms_snapshot VARCHAR(4000),
                    disclaimer_snapshot VARCHAR(4000),
                    issued_at TIMESTAMP WITH TIME ZONE,
                    accepted_at TIMESTAMP WITH TIME ZONE,
                    declined_at TIMESTAMP WITH TIME ZONE,
                    cancelled_at TIMESTAMP WITH TIME ZONE,
                    expired_at TIMESTAMP WITH TIME ZONE,
                    superseded_at TIMESTAMP WITH TIME ZONE,
                    version BIGINT NOT NULL DEFAULT 0,
                    created_by_principal_id UUID NOT NULL,
                    updated_by_principal_id UUID NOT NULL,
                    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    CONSTRAINT uq_test_quote_number UNIQUE (tenant_id, quote_number),
                    CONSTRAINT fk_test_quote_case FOREIGN KEY (after_sales_case_id, tenant_id)
                        REFERENCES platform.aftersales_cases(id, tenant_id),
                    CONSTRAINT fk_test_quote_order FOREIGN KEY (service_order_id, tenant_id)
                        REFERENCES platform.service_orders(id, tenant_id),
                    CONSTRAINT ck_test_quote_branch CHECK (branch_id IS NULL OR dealer_id IS NOT NULL),
                    CONSTRAINT ck_test_quote_status CHECK (status IN
                        ('DRAFT', 'ISSUED', 'ACCEPTED', 'DECLINED', 'CANCELLED', 'EXPIRED', 'SUPERSEDED')),
                    CONSTRAINT ck_test_quote_version CHECK (version >= 0)
                )
                """);
    }

    @AfterEach
    void clearTables() {
        exec("DROP TABLE IF EXISTS platform.service_quote_lines");
        exec("DROP TABLE IF EXISTS platform.service_quotes");
        caseRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void persistsReloadsAndQueriesQuoteWithinTenant() {
        ServiceOrder order = persistOrder("SO-QUOTE-1");
        AfterSalesCase afterSalesCase = persistCase(order.getTenantId(), "CASE-QUOTE-1");
        ServiceQuote quote = newQuote(order, afterSalesCase, "Q-1");

        quoteRepository.saveAndFlush(quote);
        ServiceQuote persisted = quoteRepository.findByIdAndTenantId(
                quote.getId(), order.getTenantId()).orElseThrow();

        assertEquals(ServiceQuoteStatus.DRAFT, persisted.getStatus());
        assertEquals("USD", persisted.getCurrencyCode());
        assertEquals("Terms", persisted.getTermsSnapshot());
        assertEquals("Disclaimer", persisted.getDisclaimerSnapshot());
        assertEquals(quote.getCreatedByPrincipalId(), persisted.getCreatedByPrincipalId());
        assertEquals(quote.getUpdatedByPrincipalId(), persisted.getUpdatedByPrincipalId());
        assertNotNull(persisted.getCreatedAt());
        assertNotNull(persisted.getUpdatedAt());
        assertTrue(quoteRepository.findByIdAndTenantId(quote.getId(), UUID.randomUUID()).isEmpty());
        assertEquals(1, quoteRepository.findByServiceOrderIdAndTenantIdOrderByCreatedAtAsc(
                order.getId(), order.getTenantId()).size());
        assertEquals(1, quoteRepository.findByAfterSalesCaseIdAndTenantIdOrderByCreatedAtAsc(
                afterSalesCase.getId(), order.getTenantId()).size());
    }

    @Test
    void allowsMultipleQuotesPerServiceOrderAndEnforcesTenantNumberScope() {
        ServiceOrder order = persistOrder("SO-QUOTE-2");
        AfterSalesCase afterSalesCase = persistCase(order.getTenantId(), "CASE-QUOTE-2");
        quoteRepository.saveAndFlush(newQuote(order, afterSalesCase, "Q-SHARED"));

        assertThrows(DataIntegrityViolationException.class, () -> {
            quoteRepository.saveAndFlush(newQuote(order, afterSalesCase, "Q-SHARED"));
        });

        ServiceOrder otherOrder = persistOrder("SO-QUOTE-3", UUID.randomUUID());
        AfterSalesCase otherCase = persistCase(otherOrder.getTenantId(), "CASE-QUOTE-3");
        quoteRepository.saveAndFlush(newQuote(otherOrder, otherCase, "Q-SHARED"));

        assertEquals(1, quoteRepository.findByServiceOrderIdAndTenantIdOrderByCreatedAtAsc(
                order.getId(), order.getTenantId()).size());
        assertTrue(quoteRepository.existsByTenantIdAndQuoteNumber(
                otherOrder.getTenantId(), "Q-SHARED"));
    }

    @Test
    void rejectsCrossTenantCaseAndOrderContainment() {
        ServiceOrder order = persistOrder("SO-QUOTE-4");
        AfterSalesCase otherTenantCase = persistCase(UUID.randomUUID(), "CASE-QUOTE-4");
        ServiceQuote invalid = newQuote(order, otherTenantCase, "Q-CROSS-TENANT");

        assertThrows(DataIntegrityViolationException.class,
                () -> quoteRepository.saveAndFlush(invalid));
    }

    @Test
    void supportsNullDealerAndBranchWithoutCascadingReferencedOrders() {
        ServiceOrder order = persistOrder("SO-QUOTE-5");
        AfterSalesCase afterSalesCase = persistCase(order.getTenantId(), "CASE-QUOTE-5");
        ServiceQuote quote = newQuote(order, afterSalesCase, "Q-RESTRICT");
        quoteRepository.saveAndFlush(quote);

        assertFalse(quoteRepository.findByIdAndTenantId(
                quote.getId(), order.getTenantId()).isEmpty());
        assertThrows(DataIntegrityViolationException.class,
                () -> orderRepository.delete(order));
    }

    private ServiceQuote newQuote(ServiceOrder order, AfterSalesCase afterSalesCase, String number) {
        return ServiceQuote.create(
                UUID.randomUUID(), order.getTenantId(), null, null,
                afterSalesCase.getId(), order.getId(), number, "USD",
                OffsetDateTime.now().plusDays(30), "Terms", "Disclaimer",
                UUID.randomUUID(), OffsetDateTime.now());
    }

    private ServiceOrder persistOrder(String number) {
        return persistOrder(number, UUID.randomUUID());
    }

    private ServiceOrder persistOrder(String number, UUID tenantId) {
        return orderRepository.saveAndFlush(ServiceOrder.open(
                UUID.randomUUID(), tenantId, null, null, number,
                UUID.randomUUID(), null, OffsetDateTime.now()));
    }

    private AfterSalesCase persistCase(UUID tenantId, String number) {
        return caseRepository.saveAndFlush(AfterSalesCase.open(
                UUID.randomUUID(), tenantId, null, null, number, null,
                null, OffsetDateTime.now()));
    }

    private void exec(String sql) {
        jdbcClient.sql(sql).update();
    }
}