import { HttpErrorResponse } from '@angular/common/http';
import { HttpHeaders } from '@angular/common/http';
import { normalizeApiError } from './api-error-normalizer';

describe('normalizeApiError', () => {
  it('preserves status, safe message, and correlation ID', () => {
    const error = new HttpErrorResponse({
      status: 409,
      error: { message: 'Quote conflict', code: 'QUOTE_CONFLICT', stack: 'secret' },
      headers: new HttpHeaders({ 'X-Correlation-ID': 'corr-1' }),
    });

    expect(normalizeApiError(error)).toEqual({
      status: 409,
      message: 'Quote conflict',
      code: 'QUOTE_CONFLICT',
      correlationId: 'corr-1',
    });
  });
});