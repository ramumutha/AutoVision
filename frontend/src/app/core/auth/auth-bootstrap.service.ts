import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuthService } from './auth.service';
import { OidcAdapter } from './oidc-adapter';
import { RuntimeConfigurationService } from '../configuration/app-configuration';

@Injectable({ providedIn: 'root' })
export class AuthBootstrapService {
  private readonly configuration = inject(RuntimeConfigurationService);
  private readonly adapter = inject(OidcAdapter);
  private readonly auth = inject(AuthService);

  async initialize(): Promise<void> {
    await this.configuration.load();
    const oidc = this.configuration.configuration().oidc;
    if (!oidc.issuer || !oidc.clientId) return;

    try {
      const result = await firstValueFrom(this.adapter.checkAuth());
      if (result.isAuthenticated && result.accessToken) {
        this.auth.establishFromProvider(result);
      } else {
        this.auth.handleUnauthorized();
      }
    } catch {
      this.auth.handleUnauthorized();
    }
  }
}