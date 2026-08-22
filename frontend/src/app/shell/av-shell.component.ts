import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
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
        <av-status [label]="connectivity.isOnline() ? localization.text('online') : localization.text('offline')" [tone]="connectivity.isOnline() ? 'success' : 'warning'" />
        <span class="user">{{ auth.isAuthenticated() ? (session.session()?.userDisplayName ?? branding.branding().organizationName) : localization.text('signedOut') }}</span>
        @if (auth.isAuthenticated()) {
          <button class="auth-action" type="button" (click)="auth.logout()">{{ localization.text('signOut') }}</button>
        } @else {
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
  protected readonly connectivity = inject(ConnectivityService);
  protected readonly branding = inject(BrandingService);
  protected readonly auth = inject(AuthService);
  protected readonly localization = inject(LocalizationService);
  protected readonly session = inject(SessionService);
  protected readonly theme = inject(ThemeService);
}