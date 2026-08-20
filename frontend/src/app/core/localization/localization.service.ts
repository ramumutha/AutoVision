import { Injectable, signal } from '@angular/core';

const ENGLISH = {
  appName: 'AutoVision', workspace: 'Workspace', navigation: 'Navigation', overview: 'Overview',
  signedOut: 'Session not connected', online: 'Online', offline: 'Offline mode ready',
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