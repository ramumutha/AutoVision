import { Injectable, signal } from '@angular/core';

const ENGLISH = {
  appName: 'AutoVision', workspace: 'Workspace', navigation: 'Navigation', overview: 'Overview',
  signedOut: 'Session not connected', signIn: 'Sign in', signOut: 'Sign out', online: 'Online', offline: 'Offline mode ready',
  serviceOrder: 'Service Order', vehicle: 'Vehicle', workspaceSections: 'Service Order sections',
  serviceLines: 'Service Lines', quotes: 'Quotes', nextDelivery: 'This section will be available in the next delivery slice.',
  loadingServiceOrder: 'Loading Service Order', unableToLoad: 'Unable to load Service Order',
  notFound: 'Service Order not found', notAuthorized: 'You are not authorized to view this Service Order.',
  offlineRetry: 'You appear to be offline. Reconnect and retry the request.', tryAgain: 'Something went wrong. Please try again.', retry: 'Retry',
  quoteNumber: 'Quote Number', status: 'Status', currency: 'Currency', created: 'Created', validUntil: 'Valid Until',
  loadingQuotes: 'Loading quotes', unableToLoadQuotes: 'Unable to load quotes', quotesUnavailable: 'Quotes unavailable',
  noQuotes: 'No quotes have been created for this service order.',
  createQuote: 'Create Quote', quoteAutomaticLines: 'Eligible service lines are selected automatically when a quote is created.',
  terms: 'Terms', disclaimer: 'Disclaimer', create: 'Create', cancel: 'Cancel',
  creatingQuote: 'Creating Quote', requiredField: 'This field is required.', quoteCreateError: 'Quote could not be created',
  quoteConflict: 'Quote could not be created because the request conflicts with the current service-order state.',
  offlineCreate: 'Quote creation currently requires a connection.', quoteCreated: 'Quote created successfully',
  quoteCreatedMessage: 'The new draft quote is now visible in this list.',
  quoteDetails: 'Quote Details', backToQuotes: 'Back to Quotes', quoteLines: 'Quote Lines', description: 'Description',
  quantity: 'Quantity', unitPrice: 'Unit Price', net: 'Net', tax: 'Tax', gross: 'Gross', loadingQuoteDetails: 'Loading quote details',
  unableToLoadQuoteDetails: 'Unable to load quote details', quoteUnavailable: 'Quote unavailable',
  customerAuthorization: 'Customer Authorization', requestCustomerAuthorization: 'Request Customer Authorization',
  authorizationNumber: 'Authorization Number', customerReference: 'Customer Reference', customerDisplayName: 'Customer Display Name',
  authorizationSummary: 'Authorization Summary', requestAuthorization: 'Request Authorization', requestingAuthorization: 'Requesting Authorization',
  customerAuthorizationRequested: 'Customer authorization requested', customerAuthorizationPending: 'The request is pending customer decision.',
  authorizationRequestError: 'Authorization request could not be created', authorizationContextUnavailable: 'Authorization context unavailable',
  authorizationOffline: 'Authorization request currently requires a connection.', authorizationConflict: 'The authorization request conflicts with the current quote state.',
  customerAuthorizationHistory: 'Customer Authorization History', loadingCustomerAuthorizationHistory: 'Loading customer authorization history',
  noCustomerAuthorizationHistory: 'No customer authorization history.', unableToLoadCustomerAuthorizationHistory: 'Unable to load customer authorization history',
  authorizationHistoryUnavailable: 'Customer authorization history unavailable', requestedAt: 'Requested At', decidedAt: 'Decided At',
  decisionChannel: 'Decision Channel', decisionReference: 'Decision Reference',
  recordCustomerAuthorization: 'Record Customer Authorization', recordCustomerDecline: 'Record Customer Decline',
  recordCustomerDeferral: 'Record Customer Deferral', withdrawAuthorizationRequest: 'Withdraw Authorization Request',
  recordingDecision: 'Recording Decision', customerAuthorizationDecisionRecorded: 'Customer authorization decision recorded', customerAuthorizationDecisionRecordedMessage: 'The customer authorization decision was recorded.',
  authorizationDecisionError: 'Authorization decision could not be recorded', authorizationDecisionConflict: 'Authorization decision conflicts with the current authorization state',
  authorizationDecisionContextUnavailable: 'Authorization decision context unavailable', authorizationDecisionOffline: 'Authorization decision currently requires a connection',
  fieldTooLong: 'This field is too long.',
  updated: 'Updated', issuedAt: 'Issued At', acceptedAt: 'Accepted At', declinedAt: 'Declined At', cancelledAt: 'Cancelled At', expiredAt: 'Expired At', supersededAt: 'Superseded At',
  line: 'Line', lineType: 'Line Type', unitOfMeasure: 'Unit of Measure', jobContext: 'Job Context', commercialData: 'Commercial Data',
  commercialAvailable: 'Commercial data available', commercialIncomplete: 'Commercial data incomplete', notAvailable: 'Not available',
  noServiceLines: 'No service lines have been added to this service order.',
  serviceJobs: 'Service Jobs', operationalAuthorization: 'Operational Authorization', noServiceJobs: 'No service jobs are available.',
  loadingAuthorizationEvaluation: 'Loading authorization evaluation', unableToLoadAuthorizationEvaluation: 'Unable to load authorization evaluation',
  authorizationEvaluationUnavailable: 'Authorization evaluation unavailable', authorizationNotRequired: 'Authorization is not required for this job.',
  totalJobLines: 'Total Job Lines', authorizedLines: 'Authorized Lines', pendingLines: 'Pending Lines', notAuthorizedLines: 'Not Authorized Lines',
  loadingAuthorizationReadiness: 'Loading authorization readiness', unableToLoadAuthorizationReadiness: 'Unable to load authorization readiness',
  authorizationReadinessUnavailable: 'Authorization readiness unavailable', ready: 'Ready', blocked: 'Blocked',
  authorizationReadinessNotRequired: 'Authorization not required', authorizationReadinessFullyAuthorized: 'Fully authorized',
  authorizationReadinessPartial: 'Partial authorization', authorizationReadinessPending: 'Authorization pending', authorizationReadinessMissing: 'Authorization missing',
  serviceProfit: 'Service Profit', serviceProfitManager: 'Service Profit Manager',
  serviceProfitContext: 'Actionable service revenue opportunities, their potential value, and the evidence behind them.',
  refresh: 'Refresh', refreshing: 'Refreshing', refreshServiceProfit: 'Refresh Service Profit data', loadingServiceProfit: 'Loading Service Profit opportunities', unableToLoadServiceProfit: 'Unable to load Service Profit',
  unableToRefreshServiceProfit: 'Unable to refresh Service Profit', existingServiceProfitDataRetained: 'The previously loaded data is still available. Try refreshing again.',
  recoverableCommercialValue: 'Recoverable commercial value', recoverablePotential: 'Recoverable Potential',
  totalOpportunities: 'Total Opportunities', highPriority: 'High Priority', reviewRequired: 'Review Required', readyToAction: 'Ready to Action',
  opportunitiesNeedingAttention: 'Opportunities needing attention', opportunityQueueContext: 'Select an opportunity to review why it was identified and what to do next.',
  priority: 'Priority', opportunityType: 'Opportunity Type', opportunityTypes: 'Opportunity types', opportunityFilters: 'Opportunity filters', actionability: 'Actionability', all: 'All', opportunity: 'Opportunity',
  sortBy: 'Sort by', newest: 'Newest', oldest: 'Oldest', highestPotential: 'Highest potential', lowestPotential: 'Lowest potential',
  evidenceStrength: 'Evidence Strength', potential: 'Potential', detected: 'Detected', noOpportunities: 'No opportunities match these filters',
  opportunitySummaries: 'Opportunity summaries', evidence: 'evidence', openDetails: 'Open details',
  sortAndFilter: 'Sort & Filter', applyFilters: 'Apply', clearFilters: 'Clear', activeFilters: 'active filters',
  noOpportunitiesContext: 'Change a filter or refresh to check for newly detected opportunities.', loadingOpportunity: 'Loading opportunity details',
  unableToLoadOpportunity: 'Unable to load opportunity details', detailSessionExpired: 'Your session could not be renewed. Sign in again to view this opportunity.', detailNotAuthorized: 'You are not authorized to view this opportunity.', opportunityDetail: 'Opportunity detail', selected: 'Selected', suppressed: 'Suppressed',
  backToOpportunities: 'Back to opportunities', opportunityUnavailable: 'Opportunity unavailable', opportunityNotFound: 'This opportunity could not be found or is no longer available.',
  suppressedOpportunity: 'Suppressed opportunity', doNotAction: 'Do not action', reviewBeforeCustomerContact: 'Review is required before customer contact.', whyAutoVisionFoundThis: 'Why AutoVision found this', evidenceBasis: 'Evidence basis',
  recommendedAction: 'Recommended action', evidenceAndAuditDetails: 'Evidence & audit details', sourceAndProvenance: 'Source and provenance', sourceSystem: 'Source system',
  sourceEntityType: 'Source entity type', sourceEntityId: 'Source entity ID', policyVersion: 'Policy version',
  customer: 'Customer', customerRef: 'Customer Ref', phone: 'Phone', email: 'Email', contactable: 'Contactable', yes: 'Yes', contactInformationIncomplete: 'Contact information incomplete',
  registration: 'Registration', vin: 'VIN', makeAndModel: 'Make and model', modelYear: 'Model year', powertrain: 'Powertrain',
  serviceContextHeading: 'Service context', repairOrder: 'Repair Order', serviceDate: 'Service date', serviceItem: 'Service item', advisorContext: 'Advisor context',
  userProfile: 'User profile', signedInAs: 'Signed in as',
};

@Injectable({ providedIn: 'root' })
export class LocalizationService {
  readonly locale = signal('en');
  private readonly supported = new Set(['en']);

  bootstrap(userPreference?: string): void {
    const browserLocale = typeof navigator === 'undefined' ? undefined : navigator.language.split('-')[0];
    const selected = userPreference ?? browserLocale ?? 'en';
    this.locale.set(this.supported.has(selected) ? selected : 'en');
  }

  text(key: keyof typeof ENGLISH): string { return ENGLISH[key]; }
}