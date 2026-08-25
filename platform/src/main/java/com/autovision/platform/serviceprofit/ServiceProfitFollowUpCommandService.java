package com.autovision.platform.serviceprofit;

import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ServiceProfitFollowUpCommandService {

    private final ServiceProfitOpportunityAccessService opportunityAccessService;
    private final ServiceProfitFollowUpPersistenceService persistenceService;
    private final ServiceProfitFollowUpRepository followUpRepository;
    private final ServiceProfitFollowUpHistoryRepository historyRepository;
    private final AuthorizationService authorizationService;

    public ServiceProfitFollowUpCommandService(
            ServiceProfitOpportunityAccessService opportunityAccessService,
            ServiceProfitFollowUpPersistenceService persistenceService,
            ServiceProfitFollowUpRepository followUpRepository,
            ServiceProfitFollowUpHistoryRepository historyRepository,
            AuthorizationService authorizationService
    ) {
        this.opportunityAccessService = opportunityAccessService;
        this.persistenceService = persistenceService;
        this.followUpRepository = followUpRepository;
        this.historyRepository = historyRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public ServiceProfitFollowUpResult ensure(
            AuthenticatedTenantContext context,
            UUID opportunityId
    ) {
        requireContext(context);
        ServiceProfitOpportunity opportunity = requireOpportunity(context, opportunityId, false);
        requireEnsureActionability(opportunity);
        return result(persistenceService.create(
                context.tenantId(), opportunityId, context.userRefId(), OffsetDateTime.now()));
    }

    @Transactional
    public ServiceProfitFollowUpResult selfClaim(
            AuthenticatedTenantContext context,
            UUID opportunityId,
            long expectedVersion
    ) {
        ServiceProfitFollowUp followUp = requireMutableFollowUp(context, opportunityId, expectedVersion);
        if (followUp.getOwnerPrincipalId() != null) {
            if (followUp.getOwnerPrincipalId().equals(context.userRefId())) return result(followUp);
            throw conflict("Follow-up is owned by another principal");
        }

        OffsetDateTime now = OffsetDateTime.now();
        long observedVersion = followUp.getVersion();
        followUp.claim(context.userRefId(), now);
        return saveWithHistory(followUp, context, ServiceProfitFollowUpEventType.OWNERSHIP_CLAIMED,
                null, context.userRefId().toString(), observedVersion, now);
    }

    @Transactional
    public ServiceProfitFollowUpResult setNextActionDueAt(
            AuthenticatedTenantContext context,
            UUID opportunityId,
            OffsetDateTime dueAt,
            long expectedVersion
    ) {
        if (dueAt == null) throw badRequest("Next action due time is required");
        ServiceProfitOpportunity opportunity = requireOpportunity(context, opportunityId, false);
        requireReadyForOperationalHandling(opportunity);
        ServiceProfitFollowUp followUp = requireFollowUp(context, opportunityId);
        checkVersion(followUp, expectedVersion);
        if (dueAt.equals(followUp.getNextActionDueAt())) return result(followUp);
        requireOwner(context, followUp);

        OffsetDateTime now = OffsetDateTime.now();
        long observedVersion = followUp.getVersion();
        String previous = value(followUp.getNextActionDueAt());
        followUp.changeDueAt(dueAt, now);
        return saveWithHistory(followUp, context, ServiceProfitFollowUpEventType.DUE_DATE_CHANGED,
                previous, dueAt.toString(), observedVersion, now);
    }

    @Transactional
    public ServiceProfitFollowUpResult recordDisposition(
            AuthenticatedTenantContext context,
            UUID opportunityId,
            ServiceProfitFollowUpDisposition disposition,
            long expectedVersion
    ) {
        if (disposition == null) throw badRequest("Follow-up disposition is required");
        ServiceProfitOpportunity opportunity = requireOpportunity(context, opportunityId, false);
        requireReadyForOperationalHandling(opportunity);
        ServiceProfitFollowUp followUp = requireFollowUp(context, opportunityId);
        checkVersion(followUp, expectedVersion);
        if (disposition == followUp.getCurrentDisposition()) return result(followUp);
        requireOwner(context, followUp);

        OffsetDateTime now = OffsetDateTime.now();
        long observedVersion = followUp.getVersion();
        String previous = followUp.getCurrentDisposition().name();
        followUp.changeDisposition(disposition, now);
        return saveWithHistory(followUp, context, ServiceProfitFollowUpEventType.DISPOSITION_CHANGED,
                previous, disposition.name(), observedVersion, now);
    }

    @Transactional
    public ServiceProfitFollowUpResult completeHandling(
            AuthenticatedTenantContext context,
            UUID opportunityId,
            long expectedVersion
    ) {
        ServiceProfitOpportunity opportunity = requireOpportunity(context, opportunityId, false);
        requireReadyForOperationalHandling(opportunity);
        ServiceProfitFollowUp followUp = requireFollowUp(context, opportunityId);
        checkVersion(followUp, expectedVersion);
        if (followUp.getHandlingStatus() == ServiceProfitFollowUpHandlingStatus.COMPLETED) return result(followUp);
        requireOwner(context, followUp);

        OffsetDateTime now = OffsetDateTime.now();
        long observedVersion = followUp.getVersion();
        followUp.complete(now);
        return saveWithHistory(followUp, context, ServiceProfitFollowUpEventType.HANDLING_STATUS_CHANGED,
                ServiceProfitFollowUpHandlingStatus.OPEN.name(),
                ServiceProfitFollowUpHandlingStatus.COMPLETED.name(), observedVersion, now);
    }

    @Transactional(readOnly = true)
    public ServiceProfitFollowUpResult readCurrent(
            AuthenticatedTenantContext context,
            UUID opportunityId
    ) {
        requireOpportunity(context, opportunityId, true);
        return result(requireFollowUp(context, opportunityId));
    }

    @Transactional(readOnly = true)
    public List<ServiceProfitFollowUpHistory> readHistory(
            AuthenticatedTenantContext context,
            UUID opportunityId
    ) {
        requireOpportunity(context, opportunityId, true);
        ServiceProfitFollowUp followUp = requireFollowUp(context, opportunityId);
        return List.copyOf(historyRepository.findAllByTenantIdAndFollowUpIdOrderByOccurredAtAscIdAsc(
                context.tenantId(), followUp.getId()));
    }

    private ServiceProfitFollowUpResult saveWithHistory(
            ServiceProfitFollowUp followUp,
            AuthenticatedTenantContext context,
            ServiceProfitFollowUpEventType eventType,
            String previous,
            String current,
            long observedVersion,
            OffsetDateTime now
    ) {
        try {
            ServiceProfitFollowUp saved = followUpRepository.saveAndFlush(followUp);
            historyRepository.save(ServiceProfitFollowUpHistory.record(
                    UUID.randomUUID(), context.tenantId(), saved.getId(), saved.getOpportunityId(),
                    context.userRefId(), eventType, previous, current, observedVersion,
                    saved.getVersion(), now));
            return result(saved);
        } catch (OptimisticLockingFailureException exception) {
            throw conflict("Follow-up version is stale");
        }
    }

    private ServiceProfitFollowUp requireMutableFollowUp(
            AuthenticatedTenantContext context,
            UUID opportunityId,
            long expectedVersion
    ) {
        ServiceProfitOpportunity opportunity = requireOpportunity(context, opportunityId, false);
        requireClaimActionability(opportunity);
        ServiceProfitFollowUp followUp = requireFollowUp(context, opportunityId);
        checkVersion(followUp, expectedVersion);
        return followUp;
    }

    private ServiceProfitOpportunity requireOpportunity(
            AuthenticatedTenantContext context,
            UUID opportunityId,
            boolean readOnly
    ) {
        requireContext(context);
        UUID requestedOpportunityId = requireId(opportunityId, "Opportunity ID is required");
        authorizationService.requirePermission(new AuthorizationRequest(
                context,
                readOnly ? ServiceProfitPermissions.FOLLOW_UP_READ : ServiceProfitPermissions.FOLLOW_UP_MANAGE,
                AuthorizationResourceType.SERVICE_PROFIT_OPPORTUNITY,
                requestedOpportunityId));
        return opportunityAccessService.requireOpportunity(context, requestedOpportunityId);
    }

    private ServiceProfitFollowUp requireFollowUp(
            AuthenticatedTenantContext context,
            UUID opportunityId
    ) {
        return followUpRepository.findByTenantIdAndOpportunityId(context.tenantId(), opportunityId)
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND, "Service Profit follow-up not found"));
    }

    private void requireOwner(
            AuthenticatedTenantContext context,
            ServiceProfitFollowUp followUp
    ) {
        if (!context.userRefId().equals(followUp.getOwnerPrincipalId())) {
            throw conflict("Follow-up must be claimed by the current principal");
        }
    }

    private void checkVersion(ServiceProfitFollowUp followUp, long expectedVersion) {
        if (expectedVersion < 0 || followUp.getVersion() != expectedVersion) {
            throw conflict("Follow-up version is stale");
        }
    }

    private void requireEnsureActionability(ServiceProfitOpportunity opportunity) {
        if (opportunity.getActionability() == ServiceProfitActionability.SUPPRESSED
                || opportunity.getActionability() == ServiceProfitActionability.BLOCKED
                || opportunity.getActionability() == ServiceProfitActionability.CONTACT_DATA_MISSING) {
            throw badRequest("Opportunity is not eligible for internal follow-up");
        }
        requireNonTerminalStatus(opportunity);
    }

    private void requireClaimActionability(ServiceProfitOpportunity opportunity) {
        requireEnsureActionability(opportunity);
    }

    private void requireReadyForOperationalHandling(ServiceProfitOpportunity opportunity) {
        requireEnsureActionability(opportunity);
        if (opportunity.getActionability() != ServiceProfitActionability.READY) {
            throw badRequest("Opportunity requires review before operational handling");
        }
    }

    private void requireNonTerminalStatus(ServiceProfitOpportunity opportunity) {
        if (opportunity.getStatus() == ServiceProfitOpportunityStatus.INVALID
                || opportunity.getStatus() == ServiceProfitOpportunityStatus.DUPLICATE
                || opportunity.getStatus() == ServiceProfitOpportunityStatus.SUPPRESSED
                || opportunity.getStatus() == ServiceProfitOpportunityStatus.EXPIRED
                || opportunity.getStatus() == ServiceProfitOpportunityStatus.CLOSED
                || opportunity.getStatus() == ServiceProfitOpportunityStatus.REJECTED) {
            throw badRequest("Opportunity is not eligible for internal follow-up");
        }
    }

    private void requireContext(AuthenticatedTenantContext context) {
        if (context == null || context.tenantId() == null || context.userRefId() == null) {
            throw badRequest("Authenticated tenant context is required");
        }
    }

    private UUID requireId(UUID id, String message) {
        if (id == null) throw badRequest(message);
        return id;
    }

    private ServiceProfitFollowUpResult result(ServiceProfitFollowUp followUp) {
        return ServiceProfitFollowUpResult.from(followUp);
    }

    private String value(OffsetDateTime value) {
        return value == null ? null : value.toString();
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(BAD_REQUEST, message);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(CONFLICT, message);
    }
}