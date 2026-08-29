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
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CommercialEnquiryService {
    private static final int TOKEN_BYTES = 32;
    private static final int DUPLICATE_WINDOW_DAYS = 7;
    private static final int RATE_LIMIT_PER_HOUR = 10;
    private static final Set<String> FREE_MAIL_DOMAINS = Set.of("gmail.com", "outlook.com", "hotmail.com", "yahoo.com", "icloud.com");

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
        if (enquiryRepository.existsByBusinessEmailAndPurposeAndProductAndCreatedAtAfter(email, request.purpose(), product, now.minusDays(DUPLICATE_WINDOW_DAYS))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "A similar enquiry was recently received");
        }

        CommercialEnquiry enquiry = CommercialEnquiry.submit(UUID.randomUUID(), request.purpose(), clean(request.companyName()),
                clean(request.firstName()), clean(request.lastName()), email, classify(email), clean(request.roleOrTitle()),
                clean(request.countryOrMarket()), clean(request.messageOrRequirement()), request.purpose() == CommercialEnquiryPurpose.PRODUCT_DEMO ? request.productFamily() : null,
                product, request.purpose() == CommercialEnquiryPurpose.ADVISORY_IMPLEMENTATION ? request.advisoryArea() : null, now);
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
