import { Injectable, signal } from '@angular/core';

const ENGLISH = {
  appName: 'AutoVision', workspace: 'Workspace', navigation: 'Navigation', overview: 'Overview',
  signedOut: 'Session not connected', online: 'Online', offline: 'Offline mode ready',
  serviceOrder: 'Service Order', vehicle: 'Vehicle', workspaceSections: 'Service Order sections',
  serviceLines: 'Service Lines', quotes: 'Quotes', nextDelivery: 'This section will be available in the next delivery slice.',
  loadingServiceOrder: 'Loading Service Order', unableToLoad: 'Unable to load Service Order',
  notFound: 'Service Order not found', notAuthorized: 'You are not authorized to view this Service Order.',
  offlineRetry: 'You appear to be offline. Reconnect and retry the request.', tryAgain: 'Something went wrong. Please try again.', retry: 'Retry',
  quoteNumber: 'Quote Number', status: 'Status', currency: 'Currency', created: 'Created', validUntil: 'Valid Until',
  loadingQuotes: 'Loading quotes', unableToLoadQuotes: 'Unable to load quotes', quotesUnavailable: 'Quotes unavailable',
  noQuotes: 'No quotes have been created for this service order.',
  createQuote: 'Create Quote', quoteAutomaticLines: 'Eligible Service Lines are resolved automatically by the backend.',
  terms: 'Terms', disclaimer: 'Disclaimer', create: 'Create', cancel: 'Cancel',
  creatingQuote: 'Creating Quote', requiredField: 'This field is required.', quoteCreateError: 'Quote could not be created',
  quoteConflict: 'Quote could not be created because the request conflicts with the current service-order state.',
  offlineCreate: 'Quote creation currently requires a connection.', quoteCreated: 'Quote created successfully',
  quoteCreatedMessage: 'The new draft quote is now visible in this list.',
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