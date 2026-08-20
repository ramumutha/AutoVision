import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from './api-error';

export function normalizeApiError(error: unknown): ApiError {
  if (error instanceof HttpErrorResponse) {
    const body = typeof error.error === 'object' && error.error !== null ? error.error : {};
    return {
      status: error.status,
      message: typeof body['message'] === 'string' ? body['message'] : 'The request could not be completed.',
      code: typeof body['code'] === 'string' ? body['code'] : undefined,
      correlationId: error.headers.get('X-Correlation-ID') ?? undefined,
    };
  }
  return { status: 0, message: 'The request could not be completed.' };
}