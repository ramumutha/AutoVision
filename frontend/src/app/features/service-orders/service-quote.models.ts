export type ServiceQuoteStatus = 'DRAFT' | 'ISSUED' | 'ACCEPTED' | 'DECLINED' | 'CANCELLED' | 'EXPIRED' | 'SUPERSEDED';

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