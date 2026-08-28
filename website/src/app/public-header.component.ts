import { ChangeDetectionStrategy, Component, HostListener, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { filter } from 'rxjs';
import { ProductDemoService } from './product-demo.service';

@Component({
  selector: 'site-header',
  imports: [RouterLink, RouterLinkActive],
  template: `
    <header class="header">
      <a class="brand" routerLink="/" aria-label="AutoVision by VERSPEN home"><span class="master">VERSPEN</span><span class="divider">/</span><span>AutoVision</span></a>
      <button #menuButton class="menu-button" type="button" [attr.aria-expanded]="menuOpen()" aria-controls="site-navigation" (click)="toggleMenu()">Menu</button>
      <nav id="site-navigation" [class.open]="menuOpen()" aria-label="Primary navigation">
        <a routerLink="/autovision" routerLinkActive="active">AutoVision</a>
        <a routerLink="/service-profit-ai" routerLinkActive="active">Service Profit AI</a>
        <a routerLink="/for-dealers" routerLinkActive="active">For Dealers</a>
        <a routerLink="/how-it-works" routerLinkActive="active">How It Works</a>
        <a routerLink="/integration-security" routerLinkActive="active">Integration &amp; Security</a>
        <a routerLink="/about" routerLinkActive="active">About</a>
        @if (productDemo.url(); as productDemoUrl) { <a class="product-demo" [href]="productDemoUrl">Product Demo</a> }
        @else { <button class="product-demo unavailable" type="button" disabled aria-describedby="product-demo-status">Product Demo<span id="product-demo-status">Authorized access unavailable</span></button> }
        <a class="request-demo" routerLink="/request-demo">Request a Demo</a>
      </nav>
    </header>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [`
    .header { display: flex; align-items: center; gap: 2rem; max-inline-size: var(--site-wide-max); min-block-size: 5rem; margin: auto; padding: .75rem var(--site-gutter); background: var(--site-color-surface); }
    .brand { display: inline-flex; align-items: center; gap: .45rem; color: var(--site-color-ink); font-weight: 800; text-decoration: none; white-space: nowrap; }
    .master { color: var(--site-color-brand-strong); letter-spacing: .08em; font-size: .78rem; }
    .divider { color: var(--site-color-signal); }
    nav { display: flex; align-items: center; justify-content: flex-end; gap: .25rem; flex: 1; }
    nav a, nav button { min-block-size: 2.75rem; padding: .7rem .65rem; color: var(--site-color-ink-soft); font-size: var(--site-type-navigation); text-decoration: none; }
    nav a:hover, nav a.active { color: var(--site-color-brand-strong); } nav a.active { text-decoration: underline; text-decoration-thickness: 2px; text-underline-offset: .25rem; }
    .request-demo { border-radius: var(--site-radius-sm); background: var(--site-color-brand); color: white; font-weight: 800; }
    .product-demo { border: 1px solid var(--site-color-border); border-radius: var(--site-radius-sm); background: transparent; } .unavailable { display: grid; gap: 0; cursor: not-allowed; opacity: .6; text-align: start; } .unavailable span { font-size: .7rem; font-weight: 400; }
    .disabled { cursor: not-allowed; opacity: .55; }
    .menu-button { display: none; min-block-size: 2.75rem; border: 1px solid var(--site-color-border); border-radius: var(--site-radius-sm); background: var(--site-color-surface); color: var(--site-color-ink); }
    @media (max-width: 1100px) { .header { flex-wrap: wrap; } .menu-button { display: block; margin-inline-start: auto; padding-inline: .9rem; } nav { display: none; flex-basis: 100%; align-items: stretch; flex-direction: column; padding-block-start: .5rem; } nav.open { display: flex; } nav a { padding: .8rem .65rem; } }
    @media (max-width: 720px) { .header { gap: 1rem; min-block-size: 4.5rem; } .brand { font-size: .95rem; } nav a { inline-size: 100%; } }
  `],
})
export class PublicHeaderComponent {
  private readonly router = inject(Router);
  readonly menuOpen = signal(false);
  readonly productDemo = inject(ProductDemoService);

  constructor() {
    this.router.events.pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd)).subscribe(() => this.menuOpen.set(false));
  }

  toggleMenu(): void { this.menuOpen.update(open => !open); }

  @HostListener('document:keydown.escape', ['$event'])
  closeMenu(event: Event): void {
    if (this.menuOpen()) { event.preventDefault(); this.menuOpen.set(false); document.querySelector<HTMLButtonElement>('.menu-button')?.focus(); }
  }
}