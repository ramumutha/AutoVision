import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from '../core/auth/auth.service';
import { AvShellComponent } from './av-shell.component';

describe('AvShellComponent authentication actions', () => {
  let fixture: ComponentFixture<AvShellComponent>;
  let auth: { isAuthenticated: ReturnType<typeof signal<boolean>>; beginLogin: ReturnType<typeof vi.fn>; logout: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    auth = {
      isAuthenticated: signal(false),
      beginLogin: vi.fn(),
      logout: vi.fn(),
    };
    await TestBed.configureTestingModule({
      imports: [AvShellComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: auth },
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
    auth.isAuthenticated.set(true);
    fixture.detectChanges();
    const button = fixture.nativeElement.querySelector('button.auth-action') as HTMLButtonElement;

    expect(button?.textContent?.trim()).toBe('Sign out');
    button.click();

    expect(auth.logout).toHaveBeenCalledOnce();
  });
});