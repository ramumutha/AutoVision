import { Injectable, InjectionToken, signal } from '@angular/core';

export interface AppConfiguration {
  apiBaseUrl: string;
  productName: string;
  locale: string;
  environment: 'development' | 'test' | 'production';
  oidc: {
    issuer: string;
    realm: string;
    clientId: string;
  };
  brandingSource?: string;
  featureFlags: Record<string, boolean>;
  sessionPolicy?: {
    idleTimeoutMinutes?: number;
    absoluteTimeoutMinutes?: number;
  };
}

export const APP_CONFIGURATION = new InjectionToken<AppConfiguration>('APP_CONFIGURATION', {
  factory: () => defaultAppConfiguration(),
});

export function defaultAppConfiguration(): AppConfiguration {
  return {
    apiBaseUrl: '/api',
    productName: 'AutoVision',
    locale: 'en',
    environment: 'development',
    oidc: { issuer: '', realm: '', clientId: '' },
    featureFlags: {},
  };
}

@Injectable({ providedIn: 'root' })
export class RuntimeConfigurationService {
  readonly configuration = signal<AppConfiguration>(defaultAppConfiguration());

  async load(): Promise<void> {
    try {
      const response = await fetch('/assets/app-config.json', { cache: 'no-store' });
      if (response.ok) {
        this.configuration.set(this.mergeConfiguration(await response.json() as Partial<AppConfiguration>));
      }
    } catch {
      // Development can run with platform defaults; production deployment must provide config.
    }
  }

  private mergeConfiguration(value: Partial<AppConfiguration>): AppConfiguration {
    const defaults = defaultAppConfiguration();
    return {
      ...defaults,
      ...value,
      oidc: { ...defaults.oidc, ...value.oidc },
      featureFlags: { ...defaults.featureFlags, ...value.featureFlags },
    };
  }
}