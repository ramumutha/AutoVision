package com.autovision.platform.commercial;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CommercialEnquiryService {
    private static final int TOKEN_BYTES = 32;
    private static final int DUPLICATE_WINDOW_DAYS = 7;
    private static final int RATE_LIMIT_PER_HOUR = 10;
    private static final Set<String> FREE_MAIL_DOMAINS = Set.of("gmail.com", "outlook.com", "hotmail.com", "yahoo.com", "icloud.com");
        private static final Set<String> PRODUCT_INTERESTS = Set.of("SERVICE_PROFIT_AI");
        private static final Set<String> SERVICE_PRACTICES = Set.of("AUTOMOTIVE_DEALER_TECHNOLOGY", "PRODUCT_SOLUTION_ENGINEERING",
            "QUALITY_ENGINEERING_TEST_AUTOMATION", "PRODUCT_DEFINITION_REQUIREMENTS", "AI_DIGITAL_TRANSFORMATION",
            "FINTECH_SOLUTIONS_ADVISORY", "OTHER");
        private static final Set<String> SUPPORT_TYPES = Set.of("ADVISORY_ASSESSMENT", "ARCHITECTURE_SOLUTION_DESIGN",
            "IMPLEMENTATION_DEVELOPMENT", "INTEGRATION", "TESTING_QUALITY_ENGINEERING", "PRODUCT_REQUIREMENTS_DEFINITION",
            "MODERNIZATION", "ONGOING_ENGINEERING_SUPPORT");
        private static final Set<String> BUSINESS_OBJECTIVES = Set.of("INCREASE_SERVICE_REVENUE", "RECOVER_DECLINED_DEFERRED_WORK",
            "IMPROVE_CUSTOMER_RETENTION", "IMPROVE_ADVISOR_PRODUCTIVITY", "IMPROVE_WORKSHOP_UTILIZATION",
            "IMPROVE_SERVICE_FOLLOW_UP", "IMPROVE_MANAGEMENT_VISIBILITY", "OTHER");

    private final CommercialEnquiryRepository enquiryRepository;
    private final CommercialVerificationRepository verificationRepository;
    private final CommercialVerificationDelivery delivery;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Clock clock;
    private final ConcurrentHashMap<String, RateWindow> rateWindows = new ConcurrentHashMap<>();

    @Autowired
    public CommercialEnquiryService(CommercialEnquiryRepository enquiryRepository,
            CommercialVerificationRepository verificationRepository,
            CommercialVerificationDelivery delivery) {
        this(enquiryRepository, verificationRepository, delivery, Clock.systemUTC());
    }

    CommercialEnquiryService(CommercialEnquiryRepository enquiryRepository,
            CommercialVerificationRepository verificationRepository,
            CommercialVerificationDelivery delivery, Clock clock) {
        this.enquiryRepository = enquiryRepository;
        this.verificationRepository = verificationRepository;
        this.delivery = delivery;
        this.clock = clock;
    }

    @Transactional
    public CommercialEnquiryResponse submit(CommercialEnquiryRequest request, String clientKey) {
        enforceRateLimit(clientKey);
        validatePurpose(request);
        String email = normalizeEmail(request.businessEmail());
        OffsetDateTime now = OffsetDateTime.now(clock);
        CommercialProduct product = request.purpose() == CommercialEnquiryPurpose.PRODUCT_DEMO ? request.product() : null;
        Map<String, Object> qualification = qualification(request);
        if (enquiryRepository.existsByBusinessEmailAndPurposeAndProductAndQualificationAndCreatedAtAfter(email, request.purpose(), product,
            qualification, now.minusDays(DUPLICATE_WINDOW_DAYS))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "A similar enquiry was recently received");
        }

        CommercialEnquiry enquiry = CommercialEnquiry.submit(UUID.randomUUID(), request.purpose(), clean(request.companyName()),
                clean(request.firstName()), clean(request.lastName()), email, classify(email), clean(request.roleOrTitle()),
                clean(request.countryOrMarket()), clean(request.messageOrRequirement()), request.purpose() == CommercialEnquiryPurpose.PRODUCT_DEMO ? request.productFamily() : null,
                product, request.purpose() == CommercialEnquiryPurpose.ADVISORY_IMPLEMENTATION ? request.advisoryArea() : null,
                qualification, now);
        enquiryRepository.save(enquiry);

        byte[] tokenBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(tokenBytes);
        String token = HexFormat.of().formatHex(tokenBytes);
        verificationRepository.save(CommercialVerification.issue(UUID.randomUUID(), enquiry.getId(), digest(token), now.plusHours(24)));
        delivery.deliver(enquiry.getId(), email, token);
        return new CommercialEnquiryResponse(enquiry.getId(), enquiry.getStatus(), enquiry.getCreatedAt());
    }

    @Transactional
    public boolean verify(String token) {
        if (token == null || token.length() != TOKEN_BYTES * 2) return false;
        CommercialVerification verification = verificationRepository.findByTokenDigest(digest(token)).orElse(null);
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (verification == null || !verification.usableAt(now)) return false;
        CommercialEnquiry enquiry = enquiryRepository.findById(verification.getEnquiryId()).orElse(null);
        if (enquiry == null) return false;
        verification.consume(now);
        enquiry.verify(now);
        return true;
    }

    private void validatePurpose(CommercialEnquiryRequest request) {
        if (request.purpose() == CommercialEnquiryPurpose.PRODUCT_DEMO && (request.productFamily() != CommercialProductFamily.AUTOVISION || request.product() != CommercialProduct.SERVICE_PROFIT_AI)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported Product Demo product");
        }
        if (request.purpose() == CommercialEnquiryPurpose.ADVISORY_IMPLEMENTATION && request.advisoryArea() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Advisory area is required");
        }
        if (request.purpose() != CommercialEnquiryPurpose.PRODUCT_DEMO && (request.productFamily() != null || request.product() != null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product is only valid for Product Demo");
        }
        if (request.purpose() != CommercialEnquiryPurpose.ADVISORY_IMPLEMENTATION && request.advisoryArea() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Advisory area is only valid for Advisory & Implementation");
        }
        if (request.purpose() == CommercialEnquiryPurpose.PRODUCT_DEMO && request.products() != null && !request.products().contains("SERVICE_PROFIT_AI")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A product interest is required");
        }
        if (request.evaluationPreference() != null && !Set.of("GUIDED_DEMO", "DEMO_ENVIRONMENT", "DEALER_PILOT").contains(request.evaluationPreference())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported evaluation preference");
        }
        validateCollection(request.products(), PRODUCT_INTERESTS, "product interest");
        validateCollection(request.servicePractices(), SERVICE_PRACTICES, "service practice");
        validateCollection(request.supportTypes(), SUPPORT_TYPES, "support type");
        validateCollection(request.businessObjectives(), BUSINESS_OBJECTIVES, "business objective");
    }

    private static void validateCollection(List<String> values, Set<String> allowed, String label) {
        if (values != null && values.stream().anyMatch(value -> value == null || !allowed.contains(value.trim()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported " + label);
        }
    }

    private static Map<String, Object> qualification(CommercialEnquiryRequest request) {
        Map<String, Object> value = new LinkedHashMap<>();
        putArray(value, "products", request.products());
        putArray(value, "servicePractices", request.servicePractices());
        putArray(value, "supportTypes", request.supportTypes());
        putArray(value, "businessObjectives", request.businessObjectives());
        put(value, "evaluationPreference", request.evaluationPreference());
        put(value, "organizationType", request.organizationType());
        put(value, "partnershipType", request.partnershipType());
        put(value, "projectStage", request.projectStage());
        put(value, "desiredTimeframe", request.desiredTimeframe());
        put(value, "currentTechnology", request.currentTechnology());
        put(value, "monthlyServiceOrders", request.monthlyServiceOrders());
        put(value, "historicalDataAvailability", request.historicalDataAvailability());
        put(value, "phone", request.phone());
        put(value, "preferredContactMethod", request.preferredContactMethod());
        put(value, "cityRegion", request.cityRegion());
        put(value, "companyWebsite", request.companyWebsite());
        if (request.serviceLocations() != null) value.put("serviceLocations", request.serviceLocations());
        if (request.declinedRecommendationsRecorded() != null) value.put("declinedRecommendationsRecorded", request.declinedRecommendationsRecorded());
        return value;
    }

    private static void put(Map<String, Object> target, String name, String value) { if (value != null && !value.isBlank()) target.put(name, value.trim()); }
    private static void putArray(Map<String, Object> target, String name, List<String> values) {
        if (values == null || values.isEmpty()) return;
        List<String> array = values.stream().filter(value -> value != null && !value.isBlank()).map(String::trim).distinct().sorted().toList();
        if (!array.isEmpty()) target.put(name, new ArrayList<>(array));
    }

    private void enforceRateLimit(String clientKey) {
        String key = clientKey == null || clientKey.isBlank() ? "unknown" : clientKey;
        long now = System.currentTimeMillis();
        RateWindow window = rateWindows.compute(key, (ignored, current) -> current == null || now - current.startedAt() >= 3_600_000L
                ? new RateWindow(now, 1) : new RateWindow(current.startedAt(), current.count() + 1));
        if (window.count() > RATE_LIMIT_PER_HOUR) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests");
    }

    private CommercialEmailClassification classify(String email) {
        String domain = email.substring(email.indexOf('@') + 1);
        return FREE_MAIL_DOMAINS.contains(domain) ? CommercialEmailClassification.FREE_MAIL : CommercialEmailClassification.BUSINESS_DOMAIN;
    }

    static String normalizeEmail(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank() || !normalized.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid business email is required");
        return normalized;
    }

    private static String clean(String value) { return value == null ? "" : value.trim(); }
    static String digest(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("Required digest unavailable", exception); }
    }
    private record RateWindow(long startedAt, int count) { }
}
