import { ChangeDetectionStrategy, Component, ElementRef, HostListener, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { filter } from 'rxjs';
import { ProductDemoService } from './product-demo.service';

@Component({
  selector: 'site-header',
  imports: [RouterLink, RouterLinkActive],
  template: `
    <header class="header">
      <a class="brand" routerLink="/" aria-label="SPENMER home"><span class="wordmark">SPENMER</span></a>
      <button #menuButton class="menu-button" type="button" [attr.aria-expanded]="menuOpen()" aria-controls="site-navigation" (click)="toggleMenu()">Menu</button>
      <nav id="site-navigation" [class.open]="menuOpen()" aria-label="Primary navigation">
        <div class="nav-group"><button type="button" class="nav-trigger" [class.open]="productsMenuOpen()" aria-haspopup="true" aria-controls="products-menu" [attr.aria-expanded]="productsMenuOpen()" (click)="toggleProductsMenu()">Products <span class="chevron" aria-hidden="true"></span></button>@if (productsMenuOpen()) { <div id="products-menu" class="nav-menu nav-menu-products" role="menu" aria-label="Products"><div><b>Platform</b><a role="menuitem" routerLink="/autovision">AutoVision<span>Adaptive Vehicle Service Intelligence</span></a></div><div><b>Capability</b><a role="menuitem" routerLink="/service-profit-ai">Service Profit<span>Service-profit intelligence under AutoVision</span></a></div><div><b>Portfolio</b><a role="menuitem" routerLink="/products">AutoVision Product Portfolio<span>Current and planned product information</span></a></div></div> }</div>
        <div class="nav-group"><button type="button" class="nav-trigger" [class.open]="servicesMenuOpen()" aria-haspopup="true" aria-controls="services-menu" [attr.aria-expanded]="servicesMenuOpen()" (click)="toggleServicesMenu()">Services <span class="chevron" aria-hidden="true"></span></button>@if (servicesMenuOpen()) { <div id="services-menu" class="nav-menu nav-menu-services" role="menu" aria-label="Services"><div><b>Automotive &amp; domain</b><a role="menuitem" routerLink="/services#automotive-dealer-technology">Automotive &amp; dealer technology</a><a role="menuitem" routerLink="/services#fintech-solutions-advisory">FinTech solution advisory</a></div><div><b>Product &amp; engineering</b><a role="menuitem" routerLink="/services#product-solution-engineering">Product &amp; solution engineering</a><a role="menuitem" routerLink="/services#product-definition-requirements">Product definition &amp; requirements engineering</a></div><div><b>Quality &amp; transformation</b><a role="menuitem" routerLink="/services#quality-engineering-test-automation">Quality engineering &amp; test automation</a><a role="menuitem" routerLink="/services#ai-digital-transformation">AI &amp; digital transformation</a></div></div> }</div>
        <a routerLink="/how-it-works" routerLinkActive="active" ariaCurrentWhenActive="page">How We Work</a>
        <a routerLink="/integration-security" routerLinkActive="active" ariaCurrentWhenActive="page">Trust &amp; Security</a>
        <a routerLink="/about" routerLinkActive="active" ariaCurrentWhenActive="page">About Us</a>
        <div class="nav-group"><button class="product-demo nav-trigger" [class.open]="demoMenuOpen()" type="button" aria-haspopup="true" aria-controls="product-demo-menu" [attr.aria-expanded]="demoMenuOpen()" (click)="toggleDemoMenu()">Product Demo <span class="chevron" aria-hidden="true"></span></button>@if (demoMenuOpen()) { <div class="demo-menu nav-menu" id="product-demo-menu" role="menu" aria-label="Product Demo access options"><div><b>Evaluate AutoVision</b><a role="menuitem" routerLink="/request-demo">Request a Demo</a>@for (entry of productDemo.entries(); track entry.label) { @if (entry.url) { <a class="demo-launch" role="menuitem" [href]="entry.url" target="_blank" rel="noopener noreferrer">{{ entry.label }}<span>Opens AutoVision in a new tab</span></a> } @else { <span class="unavailable" role="menuitem" aria-disabled="true">{{ entry.label }}<span>Authorized access unavailable</span></span> } }</div></div> }</div>
        <a class="request-demo" routerLink="/contact">Contact Us <span aria-hidden="true">→</span></a>
      </nav>
    </header>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [`
    .header { display: flex; align-items: center; gap: clamp(1.5rem, 3vw, 3.5rem); max-inline-size: var(--site-wide-max); min-block-size: 5rem; margin: auto; padding: .65rem var(--site-gutter); background: var(--site-color-surface); }
    .brand { display: inline-flex; align-items: center; color: var(--site-color-ink); font-family: var(--site-font-display); font-weight: 750; text-decoration: none; white-space: nowrap; }
    .wordmark { font-size: 1.05rem; letter-spacing: .16em; line-height: 1; }
    .divider { color: var(--site-color-signal); }
    nav { display: flex; align-items: center; justify-content: flex-end; gap: .15rem; flex: 1; }
    .nav-group { position: relative; }
    nav a, nav button { display: inline-flex; align-items: center; gap: .45rem; min-block-size: 2.75rem; padding: .7rem .65rem; border: 0; border-radius: var(--site-radius-sm); color: var(--site-color-ink); font-size: var(--site-type-navigation); text-decoration: none; transition: color .2s ease, background-color .2s ease; }
    nav button { background: transparent; }
    nav a:hover, nav button:hover, nav a.active { color: var(--site-color-brand-strong); background: var(--site-color-accent-subtle); } nav a.active { box-shadow: inset 0 -2px var(--site-color-signal); font-weight: 700; } nav button.open { color: var(--site-color-brand-strong); background: var(--site-color-accent-subtle); box-shadow: inset 0 -3px var(--site-color-signal); }
    nav a:focus-visible, nav button:focus-visible, .request-demo:focus-visible { outline: 3px solid var(--site-color-focus); outline-offset: 2px; }
    .chevron { inline-size: .5rem; block-size: .5rem; border-inline-end: 2px solid currentColor; border-block-end: 2px solid currentColor; transform: translateY(-2px) rotate(45deg); transition: transform .2s ease; } .nav-trigger.open .chevron { transform: translateY(2px) rotate(225deg); }
    nav a.request-demo { min-block-size: 2.75rem; margin-inline-start: .5rem; padding: .7rem 1.05rem; border-radius: 1.25rem; background: var(--site-color-surface-dark); color: var(--site-color-text-inverse); font-weight: 600; }
    nav a.request-demo:hover { background: var(--site-color-brand-strong); color: var(--site-color-text-inverse); }
    nav a.request-demo:focus-visible { background: var(--site-color-surface-dark); color: var(--site-color-text-inverse); }
    nav a.request-demo:active { background: var(--site-color-brand); color: var(--site-color-text-inverse); transform: translateY(1px); }
    .demo-menu,.nav-menu { position: absolute; z-index: 5; inset-block-start: calc(100% - .1rem); right: 0; display: grid; gap: 1.5rem; margin-block-start: 0; padding: 1.35rem; border-block-start: 3px solid var(--site-color-signal); border-inline: 1px solid var(--site-color-border); border-block-end: 1px solid var(--site-color-border); border-radius: 0 0 var(--site-radius-sm) var(--site-radius-sm); background: var(--site-color-surface); box-shadow: 0 .9rem 2rem rgb(23 36 43 / 12%); }
    .nav-menu-products { grid-template-columns: repeat(3, minmax(10rem, 1fr)); min-inline-size: 38rem; } .nav-menu-services { grid-template-columns: repeat(3, minmax(12rem, 1fr)); min-inline-size: 46rem; } .demo-menu { min-inline-size: 19rem; }
    .nav-menu > div, .demo-menu > div { display: grid; align-content: start; gap: .15rem; }
    .nav-menu b, .demo-menu b { margin-block-end: .55rem; color: var(--site-color-brand-strong); font-size: .72rem; letter-spacing: .08em; line-height: 1.2; text-transform: uppercase; }
    .demo-menu a, .demo-menu .unavailable,.nav-menu a { display: grid; gap: .2rem; min-block-size: 2.75rem; padding: .55rem .35rem; border-radius: var(--site-radius-sm); color: var(--site-color-ink); text-decoration: none; }
    .nav-menu a span { color: var(--site-color-muted); font-size: .75rem; font-weight: 400; line-height: 1.35; }
    .demo-menu a:hover,.nav-menu a:hover { color: var(--site-color-brand-strong); }
    .demo-menu span { color: var(--site-color-muted); font-size: .7rem; font-weight: 400; } .unavailable { opacity: .6; }
    .disabled { cursor: not-allowed; opacity: .55; }
    .menu-button { display: none; min-block-size: 2.75rem; padding-inline: .9rem; border: 1px solid var(--site-color-border); border-radius: 1.25rem; background: var(--site-color-surface); color: var(--site-color-ink); }
    @media (max-width: 1100px) { .header { flex-wrap: wrap; } .menu-button { display: block; margin-inline-start: auto; } nav { display: none; flex-basis: 100%; align-items: stretch; flex-direction: column; padding-block-start: .5rem; } nav.open { display: flex; } nav a,nav button { inline-size: 100%; justify-content: space-between; text-align: start; } .nav-menu,.demo-menu { position: static; grid-template-columns: 1fr; gap: 1rem; min-inline-size: 0; margin: 0 0 .5rem 1rem; padding: .75rem 1rem; box-shadow: none; } .request-demo { margin-inline-start: 0; } }
    @media (max-width: 720px) { .header { gap: 1rem; min-block-size: 4.5rem; } .wordmark { font-size: .95rem; letter-spacing: .12em; } nav a { inline-size: 100%; } }
  `],
})
export class PublicHeaderComponent {
  private readonly router = inject(Router);
  readonly menuOpen = signal(false);
  readonly demoMenuOpen = signal(false);
  readonly productsMenuOpen = signal(false);
  readonly servicesMenuOpen = signal(false);
  readonly productDemo = inject(ProductDemoService);
  private readonly elementRef = inject(ElementRef);

  constructor() {
    this.router.events.pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd)).subscribe(() => this.closeMenus());
  }

  toggleMenu(): void { this.menuOpen.update(open => !open); }
  toggleDemoMenu(): void { const open = !this.demoMenuOpen(); this.closeDisclosureMenus(); this.demoMenuOpen.set(open); }
  toggleProductsMenu(): void { const open = !this.productsMenuOpen(); this.closeDisclosureMenus(); this.productsMenuOpen.set(open); }
  toggleServicesMenu(): void { const open = !this.servicesMenuOpen(); this.closeDisclosureMenus(); this.servicesMenuOpen.set(open); }
  closeDisclosureMenus(): void { this.demoMenuOpen.set(false); this.productsMenuOpen.set(false); this.servicesMenuOpen.set(false); }
  closeMenus(): void { this.menuOpen.set(false); this.closeDisclosureMenus(); }

  @HostListener('document:click', ['$event'])
  closeOnOutsideClick(event: Event): void {
    if (event.target instanceof Node && !this.elementRef.nativeElement.contains(event.target)) this.closeDisclosureMenus();
  }

  @HostListener('document:keydown', ['$event'])
  closeMenu(event: Event): void {
    if ((event as KeyboardEvent).key !== 'Escape') return;
    if (this.demoMenuOpen()) { event.preventDefault(); this.demoMenuOpen.set(false); document.querySelector<HTMLButtonElement>('.product-demo')?.focus(); return; }
    if (this.productsMenuOpen() || this.servicesMenuOpen()) { event.preventDefault(); const triggerId = this.productsMenuOpen() ? 'products-menu' : 'services-menu'; this.closeDisclosureMenus(); document.querySelector<HTMLButtonElement>(`button[aria-controls="${triggerId}"]`)?.focus(); return; }
    if (this.menuOpen()) { event.preventDefault(); this.menuOpen.set(false); document.querySelector<HTMLButtonElement>('.menu-button')?.focus(); }
  }
}