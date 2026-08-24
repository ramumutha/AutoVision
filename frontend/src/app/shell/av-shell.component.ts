import { ChangeDetectionStrategy, Component, DestroyRef, ElementRef, HostListener, computed, inject, signal, viewChild } from '@angular/core';
import { NavigationStart, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter } from 'rxjs';
import { ConnectivityService } from '../core/connectivity/connectivity.service';
import { BrandingService } from '../core/branding/branding.service';
import { AuthService } from '../core/auth/auth.service';
import { LocalizationService } from '../core/localization/localization.service';
import { SessionService } from '../core/session/session.service';
import { ThemeService } from '../core/theme/theme.service';
import { AvStatusComponent } from '../shared/design-system/av-status.component';

@Component({
  selector: 'app-av-shell',
  imports: [RouterLink, RouterLinkActive, RouterOutlet, AvStatusComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="topbar">
      <a class="brand" routerLink="/" aria-label="AutoVision home">
        <span class="brand-mark" aria-hidden="true">AV</span>
        <span>{{ branding.branding().organizationName }}</span>
      </a>
      <div class="session" aria-label="Session status">
        @if (!connectivity.isOnline()) {
          <av-status [label]="localization.text('offline')" tone="warning" />
        }
        @if (auth.isAuthenticated()) {
          <div class="user-menu">
            <button #userMenuTrigger class="user-menu-trigger" type="button" aria-haspopup="dialog" aria-controls="user-profile-menu"
              [attr.aria-expanded]="userMenuOpen()" (click)="toggleUserMenu()">
              <span class="user-name">{{ userDisplayName() }}</span><span aria-hidden="true">▾</span>
            </button>
            @if (userMenuOpen()) {
              <section class="profile-menu" id="user-profile-menu" role="dialog" [attr.aria-label]="localization.text('userProfile')">
                <span class="profile-label">{{ localization.text('signedInAs') }}</span>
                <strong>{{ userDisplayName() }}</strong>
                @if (auth.state().email; as email) { <span class="profile-email">{{ email }}</span> }
                <button class="profile-sign-out" type="button" (click)="signOut()">{{ localization.text('signOut') }}</button>
              </section>
            }
          </div>
        } @else {
          <span class="user">{{ localization.text('signedOut') }}</span>
          <button class="auth-action" type="button" (click)="auth.beginLogin()">{{ localization.text('signIn') }}</button>
        }
      </div>
    </header>
    <div class="shell">
      <nav class="nav" [attr.aria-label]="localization.text('navigation')">
        <p class="nav-label">{{ localization.text('workspace') }}</p>
        <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">{{ localization.text('overview') }}</a>
        <a routerLink="/service-profit" routerLinkActive="active">{{ localization.text('serviceProfit') }}</a>
      </nav>
      <main class="main" id="main-content" tabindex="-1"><router-outlet /></main>
    </div>
    <div class="notifications" aria-live="polite" aria-atomic="true"></div>
  `,
  styles: [`
    :host { display: block; min-height: 100dvh; }
    .topbar { display: flex; align-items: center; justify-content: space-between; gap: 1rem; min-height: 4.5rem; padding: .75rem clamp(1rem, 4vw, 3rem); border-bottom: 1px solid var(--av-color-border); background: var(--av-color-surface); }
    .brand { display: inline-flex; align-items: center; gap: .7rem; color: var(--av-color-ink); font-size: 1.125rem; font-weight: 800; text-decoration: none; }
    .brand-mark { display: grid; place-items: center; inline-size: 2rem; block-size: 2rem; border-radius: .25rem; background: var(--av-color-brand); color: white; font-size: .75rem; letter-spacing: .04em; }
    .session { display: flex; align-items: center; gap: .75rem; }
    .user { color: var(--av-color-muted); font-size: .875rem; }
    .auth-action { min-block-size: 2.5rem; padding: .55rem .9rem; border: 1px solid var(--av-color-brand); border-radius: var(--av-radius-sm); background: var(--av-color-brand); color: white; cursor: pointer; font: inherit; font-weight: 700; }
    .auth-action:hover { background: var(--av-color-brand-strong); border-color: var(--av-color-brand-strong); }
    .auth-action:focus-visible { outline: 3px solid var(--av-color-focus); outline-offset: 2px; }
    .user-menu { position: relative; }
    .user-menu-trigger { display: inline-flex; align-items: center; gap: .45rem; min-block-size: 2.5rem; max-inline-size: 18rem; padding: .5rem .7rem; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-sm); background: var(--av-color-surface); color: var(--av-color-ink); cursor: pointer; font: inherit; font-weight: 700; }
    .user-menu-trigger:hover { border-color: var(--av-color-brand); background: #f0f7f6; }
    .user-menu-trigger:focus-visible, .profile-sign-out:focus-visible { outline: 3px solid var(--av-color-focus); outline-offset: 2px; }
    .user-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .profile-menu { position: absolute; z-index: 10; inset-block-start: calc(100% + .45rem); inset-inline-end: 0; display: grid; gap: .35rem; inline-size: min(19rem, calc(100vw - 2rem)); padding: .85rem; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-sm); background: var(--av-color-surface); box-shadow: 0 .75rem 2rem rgb(20 43 48 / 16%); }
    .profile-label { color: var(--av-color-muted); font-size: .7rem; font-weight: 800; text-transform: uppercase; }
    .profile-email { overflow-wrap: anywhere; color: var(--av-color-muted); font-size: .8rem; }
    .profile-sign-out { min-block-size: 2.4rem; margin-block-start: .45rem; border: 1px solid var(--av-color-brand); border-radius: var(--av-radius-sm); background: var(--av-color-brand); color: white; cursor: pointer; font: inherit; font-weight: 700; }
    .shell { display: grid; grid-template-columns: 15rem minmax(0, 1fr); max-width: var(--av-content-max); min-height: calc(100dvh - 4.5rem); margin: 0 auto; }
    .nav { padding: 2rem 1rem; border-right: 1px solid var(--av-color-border); background: #eaf0f1; }
    .nav-label { margin: 0 0 .75rem .75rem; color: var(--av-color-muted); font-size: .75rem; font-weight: 800; letter-spacing: .1em; text-transform: uppercase; }
    .nav a { display: block; padding: .75rem; border-radius: var(--av-radius-sm); color: var(--av-color-ink); font-weight: 700; text-decoration: none; }
    .nav a:hover, .nav a.active { background: var(--av-color-surface); color: var(--av-color-brand-strong); }
    .main { min-width: 0; padding: 0 clamp(1rem, 4vw, 3rem); }
    @media (max-width: 720px) { .topbar { align-items: flex-start; flex-direction: column; } .session { width: 100%; justify-content: space-between; } .shell { display: block; } .nav { padding: .75rem 1rem; border-right: 0; border-bottom: 1px solid var(--av-color-border); } .nav-label { display: none; } .nav a { padding: .6rem .75rem; } .main { padding-inline: 1rem; } }
  `],
})
export class AvShellComponent {
  private readonly elementRef = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);
  protected readonly connectivity = inject(ConnectivityService);
  protected readonly branding = inject(BrandingService);
  protected readonly auth = inject(AuthService);
  protected readonly localization = inject(LocalizationService);
  protected readonly session = inject(SessionService);
  protected readonly theme = inject(ThemeService);
  protected readonly userMenuOpen = signal(false);
  private readonly userMenuTrigger = viewChild<ElementRef<HTMLButtonElement>>('userMenuTrigger');
  protected readonly userDisplayName = computed(() =>
    this.auth.state().displayName
      ?? this.auth.state().username
      ?? this.session.session()?.userDisplayName
      ?? this.branding.branding().organizationName
  );

  constructor() {
    this.router.events.pipe(
      filter((event) => event instanceof NavigationStart),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(() => this.closeUserMenu());
  }

  @HostListener('document:click', ['$event'])
  protected closeUserMenuOnOutsideClick(event: MouseEvent): void {
    if (!this.userMenuOpen()) return;
    const menu = this.elementRef.nativeElement.querySelector('.user-menu');
    if (menu && !menu.contains(event.target as Node)) this.closeUserMenu();
  }

  @HostListener('document:keydown.escape')
  protected closeUserMenuOnEscape(): void {
    if (!this.userMenuOpen()) return;
    this.closeUserMenu();
    this.userMenuTrigger()?.nativeElement.focus();
  }

  protected toggleUserMenu(): void {
    this.userMenuOpen.update((open) => !open);
  }

  protected closeUserMenu(): void {
    this.userMenuOpen.set(false);
  }

  protected signOut(): void {
    this.closeUserMenu();
    this.auth.logout();
  }
}