import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuthService } from './auth.service';
import { OidcAdapter } from './oidc-adapter';
import { RuntimeConfigurationService } from '../configuration/app-configuration';
import { CurrentUserApiService } from '../api/current-user-api.service';

@Injectable({ providedIn: 'root' })
export class AuthBootstrapService {
  private readonly configuration = inject(RuntimeConfigurationService);
  private readonly adapter = inject(OidcAdapter);
  private readonly auth = inject(AuthService);
  private readonly currentUserApi = inject(CurrentUserApiService);

  async initialize(): Promise<void> {
    try {
      await this.configuration.load();
      const oidc = this.configuration.configuration().oidc;
      if (!oidc.issuer || !oidc.clientId) return;
      const result = await firstValueFrom(this.adapter.checkAuth());
      if (result.isAuthenticated && result.accessToken) {
        this.auth.prepareProviderToken(result);
        const currentUser = await firstValueFrom(this.currentUserApi.getCurrentUser());
        if (!this.isUsableCurrentUser(currentUser)) {
          this.auth.handleUnauthorized();
          return;
        }
        this.auth.establishFromProvider(result);
      } else {
        this.auth.handleUnauthorized();
      }
    } catch {
      this.auth.handleUnauthorized();
    } finally {
      this.auth.markReady();
    }
  }

  private isUsableCurrentUser(value: unknown): value is {
    subject: string;
    issuer: string;
    userRefId: string;
    tenantId: string;
    externalUserId: string;
  } {
    if (typeof value !== 'object' || value === null) return false;
    const currentUser = value as Record<string, unknown>;
    return ['subject', 'issuer', 'userRefId', 'tenantId', 'externalUserId']
      .every((key) => typeof currentUser[key] === 'string' && currentUser[key] !== '');
  }
}