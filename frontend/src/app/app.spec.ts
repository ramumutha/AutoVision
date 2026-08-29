import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { OidcAdapter } from './core/auth/oidc-adapter';
import { App } from './app';
import { vi } from 'vitest';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        { provide: OidcAdapter, useValue: { logout: () => ({ subscribe: () => undefined }), authorize: vi.fn() } },
      ],
    })
      .compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('keeps the workspace hidden before authentication is ready', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.access-boundary')).toBeTruthy();
    expect(compiled.querySelector('app-av-shell')).toBeNull();
    expect(compiled.textContent).toContain('Checking access');
  });
});
