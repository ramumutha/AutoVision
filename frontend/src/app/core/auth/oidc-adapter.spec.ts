import { TestBed } from '@angular/core/testing';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { describe, expect, it, vi } from 'vitest';
import { OidcAdapter } from './oidc-adapter';

describe('OidcAdapter', () => {
  it('uses the v22 no-argument authorize form', () => {
    const authorize = vi.fn();
    TestBed.configureTestingModule({
      providers: [
        OidcAdapter,
        { provide: OidcSecurityService, useValue: { authorize } },
      ],
    });

    TestBed.inject(OidcAdapter).authorize();

    expect(authorize).toHaveBeenCalledOnce();
    expect(authorize).toHaveBeenCalledWith();
  });
});