import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { provideAuth, StsConfigHttpLoader, StsConfigLoader } from 'angular-auth-oidc-client';
import { HttpClient } from '@angular/common/http';
import { map } from 'rxjs';
import { authInterceptor } from './core/api/auth.interceptor';
import { RuntimeConfigurationService } from './core/configuration/app-configuration';
import { AuthBootstrapService } from './core/auth/auth-bootstrap.service';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideAuth({
      loader: {
        provide: StsConfigLoader,
        useFactory: () => new StsConfigHttpLoader(inject(HttpClient).get<Record<string, unknown>>('/assets/app-config.json').pipe(
          map((config) => ({
            authority: String((config['oidc'] as Record<string, unknown> | undefined)?.['issuer'] ?? ''),
            clientId: String((config['oidc'] as Record<string, unknown> | undefined)?.['clientId'] ?? ''),
            redirectUrl: window.location.origin,
            postLogoutRedirectUri: window.location.origin,
            responseType: 'code',
            scope: 'openid profile email',
            silentRenew: true,
            useRefreshToken: true,
            disablePkce: false,
          }))
        )),
      },
    }),
    provideAppInitializer(() => inject(AuthBootstrapService).initialize())
  ]
};
