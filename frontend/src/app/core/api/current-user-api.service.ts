import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiClientService } from './api-client.service';

export interface CurrentUserResponse {
  subject: string;
  issuer: string;
  userRefId: string;
  tenantId: string;
  externalUserId: string;
}

@Injectable({ providedIn: 'root' })
export class CurrentUserApiService {
  private readonly api = inject(ApiClientService);

  getCurrentUser(): Observable<CurrentUserResponse> {
    return this.api.get<CurrentUserResponse>('/v1/me');
  }
}