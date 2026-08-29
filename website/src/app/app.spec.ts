import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { AppComponent } from './app.component';
import { routes } from './app.routes';
import { ProductDemoService } from './product-demo.service';
import { PublicHeaderComponent } from './public-header.component';
import { RequestDemoComponent } from './request-demo.component';
import { ContactUsComponent } from './contact-us.component';
import { PageShellComponent } from './page-shell.component';
import { Meta, Title } from '@angular/platform-browser';
import { CommercialEnquiryService } from './commercial-enquiry.service';

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
    expect(service.url()).toBe('https://login.example.test/');
    vi.unstubAllGlobals();
  });

  it('renders the configured Product Demo entry in a new tab', async () => {
    const service = new ProductDemoService();
    service.url.set('https://login.example.test');
    service.entries.set([{ label: 'Service Profit AI', url: 'https://login.example.test' }]);
    await TestBed.configureTestingModule({ imports: [PublicHeaderComponent], providers: [provideRouter([]), { provide: ProductDemoService, useValue: service }] }).compileComponents();
    const fixture = TestBed.createComponent(PublicHeaderComponent);
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('.product-demo') as HTMLButtonElement).click();
    fixture.detectChanges();
    const link = fixture.nativeElement.querySelector('.demo-menu a') as HTMLAnchorElement;
    expect(link?.href).toBe('https://login.example.test/');
    expect(link?.target).toBe('_blank');
    expect(link?.rel).toContain('noopener');
    expect(fixture.nativeElement.querySelector('.unavailable')).toBeNull();
  });

  it('keeps important public routes and a not-found state', () => {
    expect(routes.map(route => route.path)).toEqual(expect.arrayContaining(['', 'autovision', 'request-demo', '**']));
    expect(routes.at(-1)?.data?.['notFound']).toBe(true);
  });

  it('routes Request a Demo into the shared Contact Us workflow', async () => {
    await TestBed.configureTestingModule({ imports: [RequestDemoComponent], providers: [provideRouter([]), Meta, Title] }).compileComponents();
    const fixture = TestBed.createComponent(RequestDemoComponent);
    fixture.detectChanges();
    const link = fixture.nativeElement.querySelector('a.back-link') as HTMLAnchorElement;
    expect(link.getAttribute('href')).toBe('/contact?purpose=product-demo&product=service-profit-ai');
    expect(fixture.nativeElement.querySelector('[role="status"]').textContent).toContain('unified Contact Us');
  });

  it('allows only approved contextual Contact Us preselection', async () => {
    await TestBed.configureTestingModule({ imports: [ContactUsComponent], providers: [Meta, Title, { provide: CommercialEnquiryService, useValue: { submit: vi.fn(), verify: vi.fn() } }, { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: { get: (key: string) => key === 'purpose' ? 'product-demo' : 'service-profit-ai' } } } }] }).compileComponents();
    const fixture = TestBed.createComponent(ContactUsComponent);
    fixture.detectChanges();
    expect(fixture.componentInstance.purpose()).toBe('PRODUCT_DEMO');
    expect(fixture.nativeElement.querySelector('select[name="product"]')).toBeTruthy();

  });

  it('fails closed for arbitrary Contact Us query values', async () => {
    await TestBed.configureTestingModule({ imports: [ContactUsComponent], providers: [Meta, Title, { provide: CommercialEnquiryService, useValue: { submit: vi.fn(), verify: vi.fn() } }, { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: { get: () => 'unexpected' } } } }] }).compileComponents();
    const fixture = TestBed.createComponent(ContactUsComponent);
    fixture.detectChanges();
    expect(fixture.componentInstance.purpose()).toBe('GENERAL_ENQUIRY');
  });

  it('submits a purpose-specific enquiry and reports success', async () => {
    const submit = vi.fn().mockResolvedValue({ id: 'id', status: 'VERIFICATION_PENDING', createdAt: 'now' });
    await TestBed.configureTestingModule({ imports: [ContactUsComponent], providers: [Meta, Title, { provide: CommercialEnquiryService, useValue: { submit, verify: vi.fn() } }, { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: { get: () => 'unexpected' } } } }] }).compileComponents();
    const fixture = TestBed.createComponent(ContactUsComponent);
    const component = fixture.componentInstance;
    component.companyName = 'Dealer'; component.firstName = 'Ada'; component.lastName = 'Lovelace'; component.businessEmail = 'ada@dealer.example';
    component.roleOrTitle = 'Owner'; component.countryOrMarket = 'IN'; component.message = 'Please contact us';
    component.submit();
    await new Promise(resolve => setTimeout(resolve));
    expect(submit).toHaveBeenCalledWith(expect.objectContaining({ purpose: 'GENERAL_ENQUIRY', messageOrRequirement: 'Please contact us' }));
    expect(component.statusMessage()).toContain('received your enquiry');
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