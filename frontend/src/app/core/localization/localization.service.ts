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
  fieldTooLong: 'This field is too long.',
  updated: 'Updated', issuedAt: 'Issued At', acceptedAt: 'Accepted At', declinedAt: 'Declined At', cancelledAt: 'Cancelled At', expiredAt: 'Expired At', supersededAt: 'Superseded At',
  line: 'Line', lineType: 'Line Type', unitOfMeasure: 'Unit of Measure', jobContext: 'Job Context', commercialData: 'Commercial Data',
  commercialAvailable: 'Commercial data available', commercialIncomplete: 'Commercial data incomplete', notAvailable: 'Not available',
  noServiceLines: 'No service lines have been added to this service order.',
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