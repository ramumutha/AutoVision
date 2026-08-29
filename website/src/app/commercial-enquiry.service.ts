import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export interface CommercialEnquiryPayload {
  purpose: string;
  companyName: string;
  firstName: string;
  lastName: string;
  businessEmail: string;
  roleOrTitle: string;
  countryOrMarket: string;
  messageOrRequirement: string;
  productFamily?: string;
  product?: string;
  advisoryArea?: string;
}

export interface CommercialEnquiryResponse {
  id: string;
  status: string;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class CommercialEnquiryService {
  private readonly http = inject(HttpClient);

  submit(payload: CommercialEnquiryPayload): Promise<CommercialEnquiryResponse> {
    return firstValueFrom(this.http.post<CommercialEnquiryResponse>('/api/v1/public/commercial-enquiries', payload));
  }

  verify(token: string): Promise<{ status: string }> {
    return firstValueFrom(this.http.get<{ status: string }>('/api/v1/public/commercial-enquiries/verify', { params: { token } }));
  }

  static messageFor(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 429) return 'This enquiry was recently received or too many attempts were made. Please try again later.';
    return 'We could not submit your enquiry. Please try again later.';
  }
}
