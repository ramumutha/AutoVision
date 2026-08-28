import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { AppComponent } from './app.component';
import { routes } from './app.routes';
import { ProductDemoService } from './product-demo.service';
import { PublicHeaderComponent } from './public-header.component';
import { RequestDemoComponent } from './request-demo.component';
import { PageShellComponent } from './page-shell.component';
import { Meta, Title } from '@angular/platform-browser';

describe('public website foundation', () => {
  it('boots without authenticated product services', async () => {
    await TestBed.configureTestingModule({ imports: [AppComponent], providers: [provideRouter(routes)] }).compileComponents();
    const fixture = TestBed.createComponent(AppComponent);
    expect(fixture.nativeElement.querySelector('site-header')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('site-footer')).toBeTruthy();
  });

  it('keeps Product Demo unavailable for empty or non-HTTPS configuration', async () => {
    const service = new ProductDemoService();
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => ({ productDemoUrl: 'http://incorrect.example' }) }));
    await service.load();
    expect(service.url()).toBeNull();
    vi.unstubAllGlobals();
  });

  it('accepts a configured HTTPS Product Demo destination', async () => {
    const service = new ProductDemoService();
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => ({ productDemoUrl: 'https://login.example.test' }) }));
    await service.load();
    expect(service.url()).toBe('https://login.example.test');
    vi.unstubAllGlobals();
  });

  it('renders Product Demo as a link only when configured', async () => {
    const service = new ProductDemoService();
    service.url.set('https://login.example.test');
    await TestBed.configureTestingModule({ imports: [PublicHeaderComponent], providers: [provideRouter([]), { provide: ProductDemoService, useValue: service }] }).compileComponents();
    const fixture = TestBed.createComponent(PublicHeaderComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.product-demo[href="https://login.example.test"]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.unavailable')).toBeNull();
  });

  it('keeps important public routes and a not-found state', () => {
    expect(routes.map(route => route.path)).toEqual(expect.arrayContaining(['', 'autovision', 'request-demo', '**']));
    expect(routes.at(-1)?.data?.['notFound']).toBe(true);
  });

  it('renders the Request Demo form as non-submitting', async () => {
    await TestBed.configureTestingModule({ imports: [RequestDemoComponent], providers: [provideRouter([]), Meta, Title] }).compileComponents();
    const fixture = TestBed.createComponent(RequestDemoComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('form').getAttribute('action')).toBeNull();
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBe(true);
    expect(fixture.nativeElement.querySelector('[role="status"]').textContent).toContain('not submitted');
  });

  it('supports disclosure, Escape close, and route-close behavior', async () => {
    await TestBed.configureTestingModule({ imports: [PublicHeaderComponent], providers: [provideRouter(routes), ProductDemoService] }).compileComponents();
    const fixture = TestBed.createComponent(PublicHeaderComponent);
    fixture.detectChanges();
    const menuButton = fixture.nativeElement.querySelector('.menu-button') as HTMLButtonElement;
    menuButton.click();
    fixture.detectChanges();
    expect(menuButton.getAttribute('aria-expanded')).toBe('true');
    document.dispatchEvent(new Event('keydown', { bubbles: true }));
    document.dispatchEvent(Object.assign(new Event('keydown', { bubbles: true }), { key: 'Escape' }));
    fixture.detectChanges();
    expect(menuButton.getAttribute('aria-expanded')).toBe('false');
    menuButton.click();
    await TestBed.inject(Router).navigateByUrl('/about');
    fixture.detectChanges();
    expect(menuButton.getAttribute('aria-expanded')).toBe('false');
  });

  it('updates title, description, and Open Graph metadata', async () => {
    await TestBed.configureTestingModule({ imports: [PageShellComponent], providers: [Meta, Title, { provide: ActivatedRoute, useValue: { snapshot: { data: { title: 'AutoVision', description: 'Test description' } } } }] }).compileComponents();
    const fixture = TestBed.createComponent(PageShellComponent);
    fixture.detectChanges();
    expect(TestBed.inject(Title).getTitle()).toContain('AutoVision');
    expect(document.querySelector('meta[name="description"]')?.getAttribute('content')).toBeTruthy();
    expect(document.querySelector('meta[property="og:title"]')?.getAttribute('content')).toContain('AutoVision');
  });
});