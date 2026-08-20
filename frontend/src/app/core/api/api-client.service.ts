import { HttpClient, HttpContext, HttpHeaders, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, Observable, throwError } from 'rxjs';
import { APP_CONFIGURATION } from '../configuration/app-configuration';
import { normalizeApiError } from '../error/api-error-normalizer';

export interface ApiRequestOptions {
  params?: HttpParams;
  context?: HttpContext;
  idempotencyKey?: string;
}

@Injectable({ providedIn: 'root' })
export class ApiClientService {
  private readonly http = inject(HttpClient);
  private readonly configuration = inject(APP_CONFIGURATION);

  get<T>(path: string, options: ApiRequestOptions = {}): Observable<T> {
    return this.request<T>('GET', path, undefined, options);
  }

  post<T>(path: string, body: unknown, options: ApiRequestOptions = {}): Observable<T> {
    return this.request<T>('POST', path, body, options);
  }

  private request<T>(method: string, path: string, body: unknown, options: ApiRequestOptions): Observable<T> {
    let headers = new HttpHeaders({
      Accept: 'application/json',
      'X-Correlation-ID': crypto.randomUUID(),
    });
    if (body !== undefined) {
      headers = headers.set('Content-Type', 'application/json');
    }
    if (options.idempotencyKey) {
      headers = headers.set('Idempotency-Key', options.idempotencyKey);
    }

    return this.http.request<T>(method, `${this.configuration.apiBaseUrl}${path}`, {
      body,
      headers,
      params: options.params,
      context: options.context,
    }).pipe(catchError((error) => throwError(() => normalizeApiError(error))));
  }
}