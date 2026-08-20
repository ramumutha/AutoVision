import { InjectionToken } from '@angular/core';

export interface AppConfiguration {
  apiBaseUrl: string;
  productName: string;
  locale: string;
}

export const APP_CONFIGURATION = new InjectionToken<AppConfiguration>('APP_CONFIGURATION', {
  factory: () => ({ apiBaseUrl: '/api', productName: 'AutoVision', locale: 'en' }),
});