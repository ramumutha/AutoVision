import { Injectable, signal } from '@angular/core';

const ENGLISH = {
  appName: 'AutoVision', workspace: 'Workspace', navigation: 'Navigation', overview: 'Overview',
  signedOut: 'Session not connected', online: 'Online', offline: 'Offline mode ready',
};

@Injectable({ providedIn: 'root' })
export class LocalizationService {
  readonly locale = signal('en');
  text(key: keyof typeof ENGLISH): string { return ENGLISH[key]; }
}