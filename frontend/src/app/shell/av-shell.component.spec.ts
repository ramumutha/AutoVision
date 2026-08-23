import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from '../core/auth/auth.service';
import { ConnectivityService } from '../core/connectivity/connectivity.service';
import { AvShellComponent } from './av-shell.component';

describe('AvShellComponent authentication actions', () => {
  let fixture: ComponentFixture<AvShellComponent>;
  let auth: {
    state: ReturnType<typeof signal>;
    isAuthenticated: ReturnType<typeof signal<boolean>>;
    beginLogin: ReturnType<typeof vi.fn>;
    logout: ReturnType<typeof vi.fn>;
  };
  let isOnline: ReturnType<typeof signal<boolean>>;

  beforeEach(async () => {
    isOnline = signal(true);
    auth = {
      state: signal({ status: 'UNAUTHENTICATED', roles: [] }),
      isAuthenticated: signal(false),
      beginLogin: vi.fn(),
      logout: vi.fn(),
    };
    await TestBed.configureTestingModule({
      imports: [AvShellComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: auth },
        { provide: ConnectivityService, useValue: { isOnline } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(AvShellComponent);
    fixture.detectChanges();
  });

  it('renders an accessible Sign in action and delegates login', () => {
    const button = fixture.nativeElement.querySelector('button.auth-action') as HTMLButtonElement;

    expect(button?.textContent?.trim()).toBe('Sign in');
    expect(button?.getAttribute('aria-label')).toBeNull();
    button.click();

    expect(auth.beginLogin).toHaveBeenCalledOnce();
  });

  it('renders Sign out for an authenticated session and delegates logout', () => {
    auth.state.set({ status: 'AUTHENTICATED', displayName: 'Demo Manager', username: 'demo-manager', email: 'manager@example.test', roles: [] });
    auth.isAuthenticated.set(true);
    fixture.detectChanges();
    const trigger = fixture.nativeElement.querySelector('button.user-menu-trigger') as HTMLButtonElement;

    expect(trigger?.textContent).toContain('Demo Manager');
    expect(trigger?.getAttribute('aria-expanded')).toBe('false');
    trigger.click();
    fixture.detectChanges();

    const menu = fixture.nativeElement.querySelector('.profile-menu') as HTMLElement;
    expect(trigger?.getAttribute('aria-expanded')).toBe('true');
    expect(menu?.textContent).toContain('Demo Manager');
    expect(menu?.textContent).toContain('manager@example.test');
    expect(menu?.textContent).not.toContain('Manager role');
    (menu.querySelector('.profile-sign-out') as HTMLButtonElement).click();

    expect(auth.logout).toHaveBeenCalledOnce();
  });

  it('hides healthy connectivity status and preserves the offline warning', () => {
    let text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).not.toContain('Online');

    isOnline.set(false);
    fixture.detectChanges();
    text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Offline mode ready');
  });
});