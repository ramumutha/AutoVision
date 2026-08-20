export type ServiceQuoteStatus = 'DRAFT' | 'ISSUED' | 'ACCEPTED' | 'DECLINED' | 'CANCELLED' | 'EXPIRED' | 'SUPERSEDED';

export interface CreateServiceQuoteRequest {
  quoteNumber: string;
  currencyCode: string;
  validUntil: string | null;
  termsSnapshot: string | null;
  disclaimerSnapshot: string | null;
}

export interface ServiceQuoteSummary {
  id: string;
  serviceOrderId: string;
  quoteNumber: string;
  status: ServiceQuoteStatus;
  currencyCode: string;
  validUntil?: string | null;
  issuedAt?: string | null;
  acceptedAt?: string | null;
  declinedAt?: string | null;
  cancelledAt?: string | null;
  expiredAt?: string | null;
  supersededAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ServiceQuoteLine {
  id: string;
  serviceQuoteId: string;
  serviceLineId: string;
  serviceJobId?: string | null;
  descriptionSnapshot: string;
  quantity: number;
  unitPrice: number;
  currencyCode: string;
  netAmount: number;
  taxAmount: number;
  grossAmount: number;
  sequence: number;
  createdAt: string;
}

export interface ServiceQuoteDetail extends ServiceQuoteSummary {
  afterSalesCaseId?: string | null;
  termsSnapshot?: string | null;
  disclaimerSnapshot?: string | null;
  lines: ServiceQuoteLine[];
}