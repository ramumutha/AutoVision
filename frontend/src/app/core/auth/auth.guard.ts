import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';
import { AuthService } from './auth.service';
import { RuntimeConfigurationService } from '../configuration/app-configuration';

export const authenticatedGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const configuration = inject(RuntimeConfigurationService).configuration();
  return auth.isAuthenticated() || configuration.environment === 'development';
};