package com.autovision.platform.aftersales;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ServiceQuoteLineRepositoryTests {

    @Autowired private ServiceQuoteRepository quoteRepository;
    @Autowired private ServiceQuoteLineRepository lineRepository;
    @Autowired private ServiceOrderRepository orderRepository;
    @Autowired private ServiceJobRepository jobRepository;
    @Autowired private ServiceLineRepository serviceLineRepository;
    @Autowired private AfterSalesCaseRepository caseRepository;
    @Autowired private JdbcClient jdbcClient;

    @BeforeEach
    void createSchema() {
        exec("DROP TABLE IF EXISTS platform.service_quote_lines");
        exec("DROP TABLE IF EXISTS platform.service_quotes");
        exec("""
                CREATE TABLE platform.service_quotes (
                    id UUID PRIMARY KEY,
                    tenant_id UUID NOT NULL,
                    after_sales_case_id UUID NOT NULL,
                    service_order_id UUID NOT NULL,
                    dealer_id UUID, branch_id UUID,
                    quote_number VARCHAR(80) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    currency_code VARCHAR(3) NOT NULL,
                    valid_until TIMESTAMP WITH TIME ZONE,
                    terms_snapshot VARCHAR(4000), disclaimer_snapshot VARCHAR(4000),
                    issued_at TIMESTAMP WITH TIME ZONE, accepted_at TIMESTAMP WITH TIME ZONE,
                    declined_at TIMESTAMP WITH TIME ZONE, cancelled_at TIMESTAMP WITH TIME ZONE,
                    expired_at TIMESTAMP WITH TIME ZONE, superseded_at TIMESTAMP WITH TIME ZONE,
                    version BIGINT NOT NULL DEFAULT 0,
                    created_by_principal_id UUID NOT NULL, updated_by_principal_id UUID NOT NULL,
                    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    UNIQUE (tenant_id, quote_number),
                    FOREIGN KEY (after_sales_case_id, tenant_id)
                        REFERENCES platform.aftersales_cases(id, tenant_id),
                    FOREIGN KEY (service_order_id, tenant_id)
                        REFERENCES platform.service_orders(id, tenant_id)
                )
                """);
        exec("""
                CREATE TABLE platform.service_quote_lines (
                    id UUID PRIMARY KEY,
                    service_quote_id UUID NOT NULL,
                    service_line_id UUID NOT NULL,
                    service_job_id UUID,
                    description_snapshot VARCHAR(500) NOT NULL,
                    quantity NUMERIC(19, 4) NOT NULL,
                    unit_price NUMERIC(19, 4) NOT NULL,
                    currency_code VARCHAR(3) NOT NULL,
                    net_amount NUMERIC(19, 4) NOT NULL,
                    tax_amount NUMERIC(19, 4) NOT NULL,
                    gross_amount NUMERIC(19, 4) NOT NULL,
                    sequence INTEGER NOT NULL,
                    version BIGINT NOT NULL DEFAULT 0,
                    created_by_principal_id UUID NOT NULL,
                    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    FOREIGN KEY (service_quote_id) REFERENCES platform.service_quotes(id),
                    FOREIGN KEY (service_line_id) REFERENCES platform.service_lines(id),
                    FOREIGN KEY (service_job_id) REFERENCES platform.service_jobs(id),
                    UNIQUE (service_quote_id, service_line_id),
                    CHECK (quantity > 0), CHECK (unit_price >= 0),
                    CHECK (net_amount >= 0), CHECK (tax_amount >= 0),
                    CHECK (gross_amount >= 0), CHECK (sequence >= 0), CHECK (version >= 0)
                )
                """);
    }

    @AfterEach
    void clearTables() {
        exec("DROP TABLE IF EXISTS platform.service_quote_lines");
        exec("DROP TABLE IF EXISTS platform.service_quotes");
        serviceLineRepository.deleteAll();
        jobRepository.deleteAll();
        caseRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void persistsAndListsLinesInSequenceOrder() {
        Fixture fixture = fixture();
        ServiceLine secondSource = serviceLineRepository.saveAndFlush(ServiceLine.create(
                UUID.randomUUID(), fixture.order().getId(), null, 2, ServiceLineType.LABOR,
                "Second brake work", BigDecimal.ONE, "EA", UUID.randomUUID(), OffsetDateTime.now()));
        ServiceQuoteLine second = newLine(fixture, secondSource.getId(), 20, null);
        ServiceQuoteLine first = newLine(fixture, fixture.serviceLine().getId(), 10, null);
        lineRepository.saveAndFlush(second);
        lineRepository.saveAndFlush(first);

        List<ServiceQuoteLine> lines = lineRepository
                .findByServiceQuoteIdOrderBySequenceAsc(fixture.quote().getId());

        assertEquals(2, lines.size());
        assertEquals(10, lines.getFirst().getSequence());
        assertEquals(20, lines.getLast().getSequence());
        assertEquals(first.getId(), lineRepository.findByIdAndServiceQuoteId(
                first.getId(), fixture.quote().getId()).orElseThrow().getId());
    }

    @Test
    void rejectsDuplicateSourceLineInSameQuoteButAllowsAnotherQuote() {
        Fixture fixture = fixture();
        lineRepository.saveAndFlush(newLine(
                fixture, fixture.serviceLine().getId(), 0, null));
        assertThrows(DataIntegrityViolationException.class,
                () -> lineRepository.saveAndFlush(newLine(
                        fixture, fixture.serviceLine().getId(), 1, null)));

        ServiceQuote otherQuote = quoteRepository.saveAndFlush(ServiceQuote.create(
                UUID.randomUUID(), fixture.order().getTenantId(), null, null,
                fixture.afterSalesCase().getId(), fixture.order().getId(), "Q-OTHER",
                "USD", null, null, null, UUID.randomUUID(), OffsetDateTime.now()));
        Fixture otherFixture = new Fixture(fixture.order(), fixture.afterSalesCase(),
                otherQuote, fixture.serviceLine(), fixture.job());
        lineRepository.saveAndFlush(newLine(
                otherFixture, fixture.serviceLine().getId(), 0, null));
        assertTrue(lineRepository.existsByServiceQuoteIdAndServiceLineId(
                otherQuote.getId(), fixture.serviceLine().getId()));
    }

    @Test
    void acceptsNullJobAndRejectsMissingJob() {
        Fixture fixture = fixture();
        ServiceQuoteLine line = newLine(
                fixture, fixture.serviceLine().getId(), 0, null);
        lineRepository.saveAndFlush(line);
        assertNull(lineRepository.findById(line.getId()).orElseThrow().getServiceJobId());

        ServiceQuoteLine missingJob = ServiceQuoteLine.create(
                UUID.randomUUID(), fixture.quote().getId(), fixture.serviceLine().getId(),
                UUID.randomUUID(), "Missing job", BigDecimal.ONE, BigDecimal.ZERO,
                "USD", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1,
                UUID.randomUUID(), OffsetDateTime.now());
        assertThrows(DataIntegrityViolationException.class,
                () -> lineRepository.saveAndFlush(missingJob));
    }

    @Test
    void databaseRejectsNonPositiveCommercialValuesAndSequence() {
        Fixture fixture = fixture();
        assertThrows(DataIntegrityViolationException.class,
                                () -> insertInvalid(fixture, "sequence", "-1"));
        assertThrows(DataIntegrityViolationException.class,
                () -> insertInvalid(fixture, "quantity", "0"));
        assertThrows(DataIntegrityViolationException.class,
                () -> insertInvalid(fixture, "unit_price", "-1"));
        assertThrows(DataIntegrityViolationException.class,
                () -> insertInvalid(fixture, "net_amount", "-1"));
        assertThrows(DataIntegrityViolationException.class,
                () -> insertInvalid(fixture, "tax_amount", "-1"));
        assertThrows(DataIntegrityViolationException.class,
                () -> insertInvalid(fixture, "gross_amount", "-1"));
    }

    private void insertInvalid(Fixture fixture, String column, String value) {
        String quantity = column.equals("quantity") ? value : "1";
        String unitPrice = column.equals("unit_price") ? value : "0";
        String netAmount = column.equals("net_amount") ? value : "0";
        String taxAmount = column.equals("tax_amount") ? value : "0";
        String grossAmount = column.equals("gross_amount") ? value : "0";
        String sequence = column.equals("sequence") ? value : "0";

        jdbcClient.sql("""
                INSERT INTO platform.service_quote_lines
                (id, service_quote_id, service_line_id, description_snapshot,
                 quantity, unit_price, currency_code, net_amount, tax_amount,
                 gross_amount, sequence, version, created_by_principal_id, created_at)
                VALUES (:id, :quoteId, :lineId, 'Invalid', :quantity, :unitPrice,
                        'USD', :netAmount, :taxAmount, :grossAmount, :sequence,
                        0, :principal, :createdAt)
                """)
                .param("id", UUID.randomUUID()).param("quoteId", fixture.quote().getId())
                .param("lineId", UUID.randomUUID()).param("principal", UUID.randomUUID())
                .param("quantity", new BigDecimal(quantity))
                .param("unitPrice", new BigDecimal(unitPrice))
                .param("netAmount", new BigDecimal(netAmount))
                .param("taxAmount", new BigDecimal(taxAmount))
                .param("grossAmount", new BigDecimal(grossAmount))
                .param("sequence", Integer.valueOf(sequence))
                .param("createdAt", OffsetDateTime.now()).update();
    }

    private ServiceQuoteLine newLine(
            Fixture fixture,
            UUID serviceLineId,
            int sequence,
            UUID jobId
    ) {
        return ServiceQuoteLine.create(
                UUID.randomUUID(), fixture.quote().getId(), serviceLineId, jobId,
                "Brake work", BigDecimal.ONE, new BigDecimal("10.00"), "USD",
                new BigDecimal("10.00"), BigDecimal.ZERO, new BigDecimal("10.00"), sequence,
                UUID.randomUUID(), OffsetDateTime.now());
    }

    private Fixture fixture() {
        ServiceOrder order = orderRepository.saveAndFlush(ServiceOrder.open(
                UUID.randomUUID(), UUID.randomUUID(), null, null, "SO-LINE-QUOTE-" + UUID.randomUUID(),
                UUID.randomUUID(), null, OffsetDateTime.now()));
        AfterSalesCase afterSalesCase = caseRepository.saveAndFlush(AfterSalesCase.open(
                UUID.randomUUID(), order.getTenantId(), null, null, "CASE-LINE-" + UUID.randomUUID(),
                null, null, OffsetDateTime.now()));
        ServiceQuote quote = quoteRepository.saveAndFlush(ServiceQuote.create(
                UUID.randomUUID(), order.getTenantId(), null, null, afterSalesCase.getId(), order.getId(),
                "Q-LINE-" + UUID.randomUUID(), "USD", null, null, null, UUID.randomUUID(), OffsetDateTime.now()));
        ServiceLine serviceLine = serviceLineRepository.saveAndFlush(ServiceLine.create(
                UUID.randomUUID(), order.getId(), null, 1, ServiceLineType.LABOR, "Brake work",
                BigDecimal.ONE, "EA", UUID.randomUUID(), OffsetDateTime.now()));
        return new Fixture(order, afterSalesCase, quote, serviceLine, null);
    }

    private void exec(String sql) { jdbcClient.sql(sql).update(); }

    private record Fixture(
            ServiceOrder order,
            AfterSalesCase afterSalesCase,
            ServiceQuote quote,
            ServiceLine serviceLine,
            ServiceJob job
    ) {}
}