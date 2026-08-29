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
        <button class="product-demo" type="button" aria-haspopup="menu" aria-controls="product-demo-menu" [attr.aria-expanded]="demoMenuOpen()" (click)="toggleDemoMenu()">Product Demo <span aria-hidden="true">▾</span></button>
        @if (demoMenuOpen()) { <div class="demo-menu" id="product-demo-menu" role="menu" aria-label="Product Demo access options">
          @for (entry of productDemo.entries(); track entry.label) {
            @if (entry.url) { <a role="menuitem" [href]="entry.url" target="_blank" rel="noopener noreferrer">{{ entry.label }}<span>Opens AutoVision in a new tab</span></a> }
            @else { <span class="unavailable" role="menuitem" aria-disabled="true">{{ entry.label }}<span>Authorized access unavailable</span></span> }
          }
        </div> }
        <a class="request-demo" routerLink="/contact">Contact Us</a>
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
    .product-demo { border: 1px solid var(--site-color-border); border-radius: var(--site-radius-sm); background: transparent; } .demo-menu { position: absolute; z-index: 5; display: grid; min-inline-size: 15rem; margin-block-start: .25rem; border: 1px solid var(--site-color-border); border-radius: var(--site-radius-sm); background: var(--site-color-surface); box-shadow: var(--site-shadow-sm); } .demo-menu a, .demo-menu .unavailable { display: grid; gap: .15rem; min-block-size: 2.75rem; padding: .7rem .8rem; color: var(--site-color-ink); text-decoration: none; } .demo-menu a:hover { background: var(--site-color-surface-subtle); } .demo-menu span { color: var(--site-color-muted); font-size: .7rem; font-weight: 400; } .unavailable { opacity: .6; }
    .disabled { cursor: not-allowed; opacity: .55; }
    .menu-button { display: none; min-block-size: 2.75rem; border: 1px solid var(--site-color-border); border-radius: var(--site-radius-sm); background: var(--site-color-surface); color: var(--site-color-ink); }
    @media (max-width: 1100px) { .header { flex-wrap: wrap; } .menu-button { display: block; margin-inline-start: auto; padding-inline: .9rem; } nav { display: none; flex-basis: 100%; align-items: stretch; flex-direction: column; padding-block-start: .5rem; } nav.open { display: flex; } nav a { padding: .8rem .65rem; } }
    @media (max-width: 720px) { .header { gap: 1rem; min-block-size: 4.5rem; } .brand { font-size: .95rem; } nav a { inline-size: 100%; } }
  `],
})
export class PublicHeaderComponent {
  private readonly router = inject(Router);
  readonly menuOpen = signal(false);
  readonly demoMenuOpen = signal(false);
  readonly productDemo = inject(ProductDemoService);

  constructor() {
    this.router.events.pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd)).subscribe(() => this.closeMenus());
  }

  toggleMenu(): void { this.menuOpen.update(open => !open); }
  toggleDemoMenu(): void { this.demoMenuOpen.update(open => !open); }
  closeMenus(): void { this.menuOpen.set(false); this.demoMenuOpen.set(false); }

  @HostListener('document:keydown.escape', ['$event'])
  closeMenu(event: Event): void {
    if (this.demoMenuOpen()) { event.preventDefault(); this.demoMenuOpen.set(false); document.querySelector<HTMLButtonElement>('.product-demo')?.focus(); return; }
    if (this.menuOpen()) { event.preventDefault(); this.menuOpen.set(false); document.querySelector<HTMLButtonElement>('.menu-button')?.focus(); }
  }
}