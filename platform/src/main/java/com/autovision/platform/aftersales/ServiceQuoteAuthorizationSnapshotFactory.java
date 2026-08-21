package com.autovision.platform.aftersales;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ServiceQuoteAuthorizationSnapshotFactory {

    private final ObjectMapper objectMapper;

    public ServiceQuoteAuthorizationSnapshotFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Snapshots create(
            ServiceQuote quote,
            List<ServiceQuoteLine> quoteLines
    ) {
        ObjectNode scope = objectMapper.createObjectNode();
        scope.put("sourceType", "SERVICE_QUOTE");
        scope.put("serviceQuoteId", quote.getId().toString());
        scope.put("serviceOrderId", quote.getServiceOrderId().toString());
        if (quote.getAfterSalesCaseId() == null) {
            scope.putNull("afterSalesCaseId");
        } else {
            scope.put("afterSalesCaseId", quote.getAfterSalesCaseId().toString());
        }
        scope.put("quoteNumber", quote.getQuoteNumber());

        ArrayNode quoteLineIds = scope.putArray("quoteLineIds");
        ObjectNode commercial = objectMapper.createObjectNode();
        commercial.put("sourceType", "SERVICE_QUOTE");
        commercial.put("serviceQuoteId", quote.getId().toString());
        commercial.put("quoteNumber", quote.getQuoteNumber());
        commercial.put("currencyCode", quote.getCurrencyCode());
        if (quote.getValidUntil() == null) {
            commercial.putNull("validUntil");
        } else {
            commercial.put("validUntil", quote.getValidUntil().toString());
        }
        ArrayNode commercialLines = commercial.putArray("lines");

        for (ServiceQuoteLine line : quoteLines) {
            quoteLineIds.add(line.getId().toString());

            ObjectNode commercialLine = objectMapper.createObjectNode();
            commercialLine.put("serviceQuoteLineId", line.getId().toString());
            commercialLine.put("serviceLineId", line.getServiceLineId().toString());
            if (line.getServiceJobId() == null) {
                commercialLine.putNull("serviceJobId");
            } else {
                commercialLine.put("serviceJobId", line.getServiceJobId().toString());
            }
            commercialLine.put("descriptionSnapshot", line.getDescriptionSnapshot());
            commercialLine.put("quantity", line.getQuantity());
            commercialLine.put("unitPrice", line.getUnitPrice());
            commercialLine.put("currencyCode", line.getCurrencyCode());
            commercialLine.put("netAmount", line.getNetAmount());
            commercialLine.put("taxAmount", line.getTaxAmount());
            commercialLine.put("grossAmount", line.getGrossAmount());
            commercialLine.put("sequence", line.getSequence());
            commercialLines.add(commercialLine);
        }

        return new Snapshots(scope, commercial);
    }

    public record Snapshots(
            ObjectNode authorizationScopeSnapshot,
            ObjectNode commercialSnapshot
    ) {
    }
}
