export type ServiceProfitOpportunityStatus =
  | 'DETECTED'
  | 'QUALIFIED'
  | 'ASSIGNED'
  | 'CONTACTED'
  | 'CLOSED'
  | 'REJECTED'
  | 'INVALID'
  | 'DUPLICATE'
  | 'SUPPRESSED'
  | 'EXPIRED';

export type ServiceProfitOpportunityType =
  | 'DECLINED_WORK'
  | 'DEFERRED_WORK'
  | 'DUE_SERVICE'
  | 'OVERDUE_SERVICE'
  | 'INACTIVE_CUSTOMER';

export type ServiceProfitPriority = 'HIGH' | 'MEDIUM' | 'LOW';

export type ServiceProfitActionability =
  | 'READY'
  | 'REVIEW_REQUIRED'
  | 'CONTACT_DATA_MISSING'
  | 'BLOCKED'
  | 'SUPPRESSED';

export type ServiceProfitEvidenceClass = 'SOURCE_CONFIRMED' | 'EVIDENCE_DERIVED' | 'POLICY_DERIVED';
export type ServiceProfitEvidenceStrength = 'STRONG' | 'MODERATE' | 'WEAK';
export type ServiceProfitOpportunitySort = 'DETECTED_DESC' | 'DETECTED_ASC' | 'POTENTIAL_DESC' | 'POTENTIAL_ASC';
export type ServiceProfitGroupBy = 'NONE' | 'OPPORTUNITY_TYPE' | 'PRIORITY' | 'ACTIONABILITY';
export type ServiceProfitWorkQueueOwnership = 'ALL' | 'MINE' | 'UNASSIGNED';
export type ServiceProfitWorkQueueDueState = 'ALL' | 'OVERDUE' | 'UPCOMING' | 'NO_DUE_DATE';
export type ServiceProfitWorkQueueHandlingStatus = 'OPEN' | 'COMPLETED';
export type ServiceProfitWorkQueueDisposition = 'NONE' | 'FOLLOW_UP_REQUIRED' | 'INTEREST_RECORDED' | 'DECLINED_RECORDED' | 'NO_RESPONSE_RECORDED' | 'NO_FURTHER_ACTION';
export type ServiceProfitWorkQueueGroupBy = 'DUE_STATE' | 'OWNERSHIP' | 'DISPOSITION' | 'PRIORITY' | 'NONE';
export type ServiceProfitWorkQueueSort = 'PRIORITY' | 'DUE_DATE' | 'OLDEST' | 'NEWEST';

export type ServiceProfitSuppressionReason =
  | 'WORK_ALREADY_COMPLETED'
  | 'ALREADY_INVOICED'
  | 'AUTHORITATIVE_COMPLETION_EVIDENCE'
  | 'DUPLICATE_OPPORTUNITY';

export interface ServiceProfitCurrencyPotential {
  currencyCode: string;
  amount: number;
}

export interface ServiceProfitOpportunityCount {
  key: string;
  count: number;
}

export interface ServiceProfitOpportunitySummary {
  totalOpportunities: number;
  highPriorityCount: number;
  reviewRequiredCount: number;
  readyCount: number;
  suppressedCount: number;
  potentialByCurrency: ServiceProfitCurrencyPotential[];
  byOpportunityType: ServiceProfitOpportunityCount[];
  byPriority: ServiceProfitOpportunityCount[];
  byActionability: ServiceProfitOpportunityCount[];
}

export interface ServiceProfitOpportunityQueueItem {
  id: string;
  tenantId: string;
  dealerId: string;
  branchId: string;
  locationId: string;
  opportunityKey: string;
  opportunityType: ServiceProfitOpportunityType;
  status: ServiceProfitOpportunityStatus;
  evidenceClass: ServiceProfitEvidenceClass;
  evidenceStrength: ServiceProfitEvidenceStrength;
  priority: ServiceProfitPriority;
  actionability: ServiceProfitActionability;
  title: string;
  potentialAmount: number;
  currencyCode: string;
  detectedAt: string;
}

export interface ServiceProfitOpportunityPage {
  items: ServiceProfitOpportunityQueueItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ServiceProfitWorkQueueItem {
  followUpId: string;
  opportunityId: string;
  handlingStatus: ServiceProfitWorkQueueHandlingStatus;
  ownerPrincipalId: string | null;
  claimedAt: string | null;
  dueAt: string | null;
  disposition: ServiceProfitWorkQueueDisposition;
  version: number;
  createdAt: string;
  updatedAt: string;
  actionability: ServiceProfitActionability;
  opportunityStatus: ServiceProfitOpportunityStatus;
  evidenceClass: ServiceProfitEvidenceClass;
  evidenceStrength: ServiceProfitEvidenceStrength;
  priority: ServiceProfitPriority;
  title: string;
  summary: string;
}

export interface ServiceProfitWorkQueuePage {
  items: ServiceProfitWorkQueueItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ServiceProfitWorkQueueGroup {
  key: string;
  items: ServiceProfitWorkQueueItem[];
}

export interface ServiceProfitWorkQueueQuery {
  handlingStatus?: ServiceProfitWorkQueueHandlingStatus;
  ownership?: ServiceProfitWorkQueueOwnership;
  dueState?: ServiceProfitWorkQueueDueState;
  disposition?: ServiceProfitWorkQueueDisposition;
  page?: number;
  size?: number;
}

export type ServiceProfitFollowUpHandlingStatus = 'OPEN' | 'COMPLETED';
export type ServiceProfitFollowUpOwnership = 'MINE' | 'ASSIGNED' | 'UNASSIGNED';
export type ServiceProfitFollowUpDueState = 'OVERDUE' | 'UPCOMING' | 'NO_DUE_DATE';
export type ServiceProfitFollowUpDisposition = 'NONE' | 'FOLLOW_UP_REQUIRED' | 'INTEREST_RECORDED' | 'DECLINED_RECORDED' | 'NO_RESPONSE_RECORDED' | 'NO_FURTHER_ACTION';

export interface ServiceProfitFollowUpResponse {
  opportunityId: string;
  handlingStatus: ServiceProfitFollowUpHandlingStatus;
  ownership: ServiceProfitFollowUpOwnership;
  dueAt: string | null;
  dueState: ServiceProfitFollowUpDueState;
  disposition: ServiceProfitFollowUpDisposition;
  version: number;
}

export type ServiceProfitFollowUpHistoryEventType = 'CREATED' | 'OWNERSHIP_CLAIMED' | 'DUE_DATE_CHANGED' | 'DISPOSITION_CHANGED' | 'HANDLING_STATUS_CHANGED';
export type ServiceProfitFollowUpHistoryActorType = 'HUMAN' | 'SYSTEM';

export interface ServiceProfitFollowUpHistoryResponse {
  eventType: ServiceProfitFollowUpHistoryEventType;
  actorType: ServiceProfitFollowUpHistoryActorType;
  actorIdentity: 'ME' | 'HUMAN' | 'AUTOVISION_SERVICE_PROFIT';
  applicationIdentity: 'AUTOVISION_SERVICE_PROFIT' | null;
  occurredAt: string;
  previousValue: string | null;
  newValue: string | null;
}

export interface ServiceProfitOpportunityGroup {
  key: string;
  authoritativeCount: number;
  opportunities: ServiceProfitOpportunityQueueItem[];
  complete: boolean;
}

export interface ServiceProfitOpportunityExplanation {
  headline: string;
  rationale: string;
  evidenceBasis: string;
  recommendedAction: string;
}

export interface ServiceProfitCustomerContext {
  displayName: string | null;
  reference: string | null;
  phone: string | null;
  email: string | null;
  contactable: boolean | null;
}

export interface ServiceProfitVehicleContext {
  registration: string | null;
  vin: string | null;
  make: string | null;
  model: string | null;
  modelYear: number | null;
  powertrain: string | null;
}

export interface ServiceProfitServiceContext {
  orderReference: string | null;
  serviceDate: string | null;
  description: string | null;
  advisorContext: string | null;
}

export interface ServiceProfitOpportunityContext {
  customer: ServiceProfitCustomerContext | null;
  vehicle: ServiceProfitVehicleContext | null;
  service: ServiceProfitServiceContext | null;
}

export interface ServiceProfitOpportunityResponse extends ServiceProfitOpportunityQueueItem {
  customerId: string | null;
  vehicleId: string | null;
  summary: string;
  sourceSystem: string;
  sourceEntityType: string;
  sourceEntityId: string;
  sourceServiceOrderId: string | null;
  sourceServiceJobId: string | null;
  sourceServiceLineId: string | null;
  sourceQuoteId: string | null;
  policyVersion: string;
  suppressionReason: ServiceProfitSuppressionReason | null;
  suppressedAt: string | null;
  explanation: ServiceProfitOpportunityExplanation;
  version: number;
  createdAt: string;
  updatedAt: string;
  context?: ServiceProfitOpportunityContext | null;
}

export interface ServiceProfitOpportunityFilters {
  status?: ServiceProfitOpportunityStatus;
  priority?: ServiceProfitPriority;
  opportunityType?: ServiceProfitOpportunityType;
  evidenceClass?: ServiceProfitEvidenceClass;
  evidenceStrength?: ServiceProfitEvidenceStrength;
  actionability?: ServiceProfitActionability;
  dealerId?: string;
  branchId?: string;
  locationId?: string;
}

export interface ServiceProfitOpportunityQuery extends ServiceProfitOpportunityFilters {
  page?: number;
  size?: number;
  sort?: ServiceProfitOpportunitySort;
}
export type ServiceProfitDataCapabilityStatus =
  | 'AVAILABLE'
  | 'PARTIAL'
  | 'UNAVAILABLE';

export type ServiceProfitDataCapability =
  | 'REVENUE_ATTRIBUTION'
  | 'GROSS_PROFIT_ATTRIBUTION';

export type ServiceProfitDataAssessmentState =
  | 'ASSESSED'
  | 'NOT_ASSESSED';

export interface ServiceProfitDataCapabilityItem {
  capability: ServiceProfitDataCapability;
  status: ServiceProfitDataCapabilityStatus;
  reason: string;
}

export interface ServiceProfitDataCapabilityResponse {
  assessmentState: ServiceProfitDataAssessmentState;
  sourceDatasetId: string | null;
  sourceDatasetVersion: string | null;
  assessmentPolicyVersion: string | null;
  assessedAt: string | null;
  capabilities: ServiceProfitDataCapabilityItem[];
}
