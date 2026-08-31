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
import { DEFAULT_SITE_LOCALE } from './site-locale';

describe('public website foundation', () => {
  it('boots without authenticated product services', async () => {
    await TestBed.configureTestingModule({ imports: [AppComponent], providers: [provideRouter(routes)] }).compileComponents();
    const fixture = TestBed.createComponent(AppComponent);
    expect(fixture.nativeElement.querySelector('site-header')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('site-footer')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.pattern-preview')).toBeNull();
    expect(document.documentElement.lang).toBe(DEFAULT_SITE_LOCALE);
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
    const link = fixture.nativeElement.querySelector('.demo-launch') as HTMLAnchorElement;
    expect(link?.href).toBe('https://login.example.test/');
    expect(link?.target).toBe('_blank');
    expect(link?.rel).toContain('noopener');
    expect(fixture.nativeElement.querySelector('.unavailable')).toBeNull();
  });

  it('keeps the corporate lockup independent from AutoVision', async () => {
    await TestBed.configureTestingModule({ imports: [PublicHeaderComponent], providers: [provideRouter([]), ProductDemoService] }).compileComponents();
    const headerFixture = TestBed.createComponent(PublicHeaderComponent);
    headerFixture.detectChanges();
    expect(headerFixture.nativeElement.querySelector('.brand').textContent).toBe('SPENMER');
    expect(headerFixture.nativeElement.querySelector('.brand').textContent).not.toContain('AutoVision');
    expect(headerFixture.nativeElement.querySelector('.mark')).toBeNull();

  });

  it('uses direct About Us navigation without a Company disclosure', async () => {
    await TestBed.configureTestingModule({ imports: [PublicHeaderComponent], providers: [provideRouter([{ path: 'about', component: PublicHeaderComponent }]), ProductDemoService] }).compileComponents();
    const fixture = TestBed.createComponent(PublicHeaderComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('a[routerlink="/about"]')?.textContent).toContain('About Us');
    expect(fixture.nativeElement.querySelector('button')?.textContent).not.toContain('Company');
    expect(fixture.nativeElement.querySelector('[aria-controls="company-menu"]')).toBeNull();
    await TestBed.inject(Router).navigateByUrl('/about');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('a[routerlink="/about"]')?.getAttribute('aria-current')).toBe('page');
  });

  it('renders the opening homepage composition', async () => {
    await TestBed.configureTestingModule({ imports: [PageShellComponent], providers: [Meta, Title, { provide: ActivatedRoute, useValue: { snapshot: { data: { title: 'SPENMER', description: 'Test description' }, url: [] } } }] }).compileComponents();
    const fixture = TestBed.createComponent(PageShellComponent);
    fixture.detectChanges();
    const homepage = fixture.nativeElement.querySelector('.home-page') as HTMLElement;
    expect(homepage.querySelectorAll('h1')).toHaveLength(1);
    expect(homepage.querySelector('h1')?.textContent).toContain('Technology solutions built around real business problems.');
    expect(homepage.querySelector('.home-hero .eyebrow')?.textContent).toContain('Technology engineered around real decisions.');
    expect(homepage.querySelector('.home-hero .eyebrow')?.textContent).not.toContain('Technology Products · Advisory · Engineering');
    expect(homepage.querySelector('.home-hero-lead')?.textContent).toContain('domain-focused technology products');
    expect(homepage.querySelectorAll('.home-hero-links a')).toHaveLength(2);
    expect(homepage.querySelectorAll('.home-value-list li')).toHaveLength(4);
    expect(homepage.textContent).toContain('Two ways to create meaningful change.');
    expect(homepage.textContent).toContain('Explore Services');
    expect(homepage.querySelector('.home-hero-visual svg[aria-hidden="true"]')).not.toBeNull();
    expect(homepage.textContent).toContain('Turn service signals into decisions teams can act on.');
    const autoVision = homepage.querySelector('.home-autovision') as HTMLElement;
    expect(autoVision.querySelector('h2')?.textContent).toContain('Turn service signals into decisions teams can act on.');
    expect(autoVision.textContent).toContain('SPENMER Product');
    expect(autoVision.textContent).toContain('Adaptive Vehicle Service Intelligence Platform');
    expect(autoVision.textContent).toContain('See the signal');
    expect(autoVision.textContent).toContain('Understand the context');
    expect(autoVision.textContent).toContain('Decide the next action');
    expect(autoVision.textContent).toContain('defined integration boundaries');
    expect(autoVision.textContent).toContain('ICE · Hybrid · EV');
    expect(autoVision.textContent).toContain('Illustrative product view using synthetic demo data.');
    expect(autoVision.querySelector('a[routerlink="/autovision"]')).not.toBeNull();
    expect(autoVision.querySelectorAll('h2')).toHaveLength(1);
    expect(autoVision.textContent).not.toContain('DMS');
    expect(autoVision.textContent).not.toContain('ROI');
    expect(homepage.textContent).toContain('Turn overlooked service opportunity into actionable work.');
    expect(homepage.textContent).toContain('Technology expertise focused on the outcome.');
    expect(homepage.textContent).toContain('Built for trust. Clear about responsibility.');
    expect(homepage.textContent).toContain('Request a Demo');
    expect(homepage.textContent).toContain('Measure from a clear baseline.');
    expect(homepage.textContent).not.toContain('Claims follow evidence.');
    expect(homepage.textContent).not.toContain('ROI figure');
    expect(homepage.querySelectorAll('.practice-index li')).toHaveLength(6);
    expect(homepage.querySelectorAll('.measurement-sequence li')).toHaveLength(5);
    expect(homepage.querySelectorAll('.trust-index li')).toHaveLength(4);
    expect(homepage.querySelectorAll('.operating-sequence li')).toHaveLength(5);
    expect(homepage.querySelector('.operating-sequence')?.textContent).toContain('Listen, clarify the problem');
    expect(Array.from(homepage.querySelectorAll('.operating-sequence b')).map(stage => stage.textContent)).toEqual(['Understand', 'Design', 'Deliver', 'Measure', 'Improve']);
    expect(homepage.querySelector('.home-hero')?.textContent).not.toContain('Different by strength. United by purpose.');
    expect(homepage.querySelectorAll('.home-contact a[routerlink="/contact"]')).toHaveLength(1);
    expect(homepage.querySelector('a[routerlink="/for-dealers"]')).toBeNull();
    expect(homepage.querySelector('a[routerlink="/business-value"]')).toBeNull();
    expect(homepage.textContent).toContain('A current AutoVision capability');
    expect(homepage.textContent).not.toContain('Service Profit AI');
    expect(homepage.querySelector('.operating-sequence')).not.toBeNull();
  });

  it('keeps important public routes and a not-found state', () => {
    expect(routes.map(route => route.path)).toEqual(expect.arrayContaining(['', 'products', 'services', 'autovision', 'service-profit-ai', 'for-dealers', 'how-it-works', 'integration-security', 'about', 'contact', 'request-demo', '**']));
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

  it('requires the approved product deep link pair', async () => {
    await TestBed.configureTestingModule({ imports: [ContactUsComponent], providers: [Meta, Title, { provide: CommercialEnquiryService, useValue: { submit: vi.fn(), verify: vi.fn() } }, { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: { get: (key: string) => key === 'purpose' ? 'product-demo' : 'wrong-product' } } } }] }).compileComponents();
    const fixture = TestBed.createComponent(ContactUsComponent);
    fixture.detectChanges();
    expect(fixture.componentInstance.purpose()).toBe('GENERAL_ENQUIRY');
    expect(fixture.nativeElement.querySelector('select[name="product"]')).toBeNull();
  });

  it('switches purpose sections without leaving inactive controls in the DOM', async () => {
    await TestBed.configureTestingModule({ imports: [ContactUsComponent], providers: [Meta, Title, { provide: CommercialEnquiryService, useValue: { submit: vi.fn(), verify: vi.fn() } }, { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: { get: () => 'unexpected' } } } }] }).compileComponents();
    const fixture = TestBed.createComponent(ContactUsComponent);
    const component = fixture.componentInstance;
    component.purpose.set('PARTNERSHIP');
    fixture.detectChanges();
    const legends = Array.from(fixture.nativeElement.querySelectorAll('legend')) as HTMLLegendElement[];
    expect(legends.some(legend => legend.textContent?.includes('Partnership context'))).toBe(true);
    expect(fixture.nativeElement.querySelector('input[name="evaluationPreference"]')).toBeNull();
    component.purpose.set('GENERAL_ENQUIRY');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('textarea[name="message"]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('select[name="partnershipType"]')).toBeNull();
  });

  it('exposes semantic groups and prevents incomplete service submission', async () => {
    const submit = vi.fn();
    await TestBed.configureTestingModule({ imports: [ContactUsComponent], providers: [Meta, Title, { provide: CommercialEnquiryService, useValue: { submit, verify: vi.fn() } }, { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: { get: () => 'advisory-implementation' } } } }] }).compileComponents();
    const fixture = TestBed.createComponent(ContactUsComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('fieldset legend')?.textContent).toContain('What brings you here?');
    expect(fixture.nativeElement.querySelectorAll('fieldset').length).toBeGreaterThanOrEqual(2);
    expect(fixture.componentInstance.purposeSpecificValid()).toBe(false);
    fixture.componentInstance.submit();
    expect(submit).not.toHaveBeenCalled();
  });

  it('reports a safe failure when commercial submission is rejected', async () => {
    const submit = vi.fn().mockRejectedValue({ status: 429 });
    await TestBed.configureTestingModule({ imports: [ContactUsComponent], providers: [Meta, Title, { provide: CommercialEnquiryService, useValue: { submit, verify: vi.fn() } }, { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: { get: () => 'unexpected' } } } }] }).compileComponents();
    const fixture = TestBed.createComponent(ContactUsComponent);
    const component = fixture.componentInstance;
    component.companyName = 'Dealer'; component.firstName = 'Ada'; component.lastName = 'Lovelace'; component.businessEmail = 'ada@dealer.example';
    component.roleOrTitle = 'Owner'; component.countryOrMarket = 'IN'; component.message = 'Please contact us';
    component.submit();
    await new Promise(resolve => setTimeout(resolve));
    expect(component.statusMessage()).toContain('could not submit');
    expect(fixture.nativeElement.querySelector('[role="status"]')).toBeTruthy();
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

  it('submits product evaluation preferences and canonical product interests', async () => {
    const submit = vi.fn().mockResolvedValue({ id: 'id', status: 'VERIFICATION_PENDING', createdAt: 'now' });
    await TestBed.configureTestingModule({ imports: [ContactUsComponent], providers: [Meta, Title, { provide: CommercialEnquiryService, useValue: { submit, verify: vi.fn() } }, { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: { get: (key: string) => key === 'purpose' ? 'product-demo' : 'service-profit-ai' } } } }] }).compileComponents();
    const fixture = TestBed.createComponent(ContactUsComponent);
    const component = fixture.componentInstance;
    component.companyName = 'Dealer'; component.firstName = 'Ada'; component.lastName = 'Lovelace'; component.businessEmail = 'ada@dealer.example';
    component.roleOrTitle = 'Owner'; component.countryOrMarket = 'IN'; component.organizationType = 'DEALER_GROUP'; component.evaluationPreference = 'GUIDED_DEMO';
    component.products.push('SERVICE_PROFIT_AI', 'SERVICE_PROFIT_AI');
    component.submit();
    await new Promise(resolve => setTimeout(resolve));
    expect(submit).toHaveBeenCalledWith(expect.objectContaining({ products: ['SERVICE_PROFIT_AI'], evaluationPreference: 'GUIDED_DEMO', organizationType: 'DEALER_GROUP' }));
  });

  it('requires service practice selection and submits multiple service/support interests', async () => {
    const submit = vi.fn().mockResolvedValue({ id: 'id', status: 'VERIFICATION_PENDING', createdAt: 'now' });
    await TestBed.configureTestingModule({ imports: [ContactUsComponent], providers: [Meta, Title, { provide: CommercialEnquiryService, useValue: { submit, verify: vi.fn() } }, { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: { get: (key: string) => key === 'purpose' ? 'advisory-implementation' : 'unexpected' } } } }] }).compileComponents();
    const fixture = TestBed.createComponent(ContactUsComponent);
    const component = fixture.componentInstance;
    expect(component.purposeSpecificValid()).toBe(false);
    component.servicePractices.push('PRODUCT_SOLUTION_ENGINEERING', 'AUTOMOTIVE_DEALER_TECHNOLOGY');
    component.supportTypes.push('INTEGRATION', 'ADVISORY_ASSESSMENT');
    component.companyName = 'Partner'; component.firstName = 'Ada'; component.lastName = 'Lovelace'; component.businessEmail = 'ada@partner.example';
    component.roleOrTitle = 'Director'; component.countryOrMarket = 'IN'; component.organizationType = 'CONSULTING_IMPLEMENTATION_PARTNER';
    component.submit();
    await new Promise(resolve => setTimeout(resolve));
    expect(submit).toHaveBeenCalledWith(expect.objectContaining({ servicePractices: ['AUTOMOTIVE_DEALER_TECHNOLOGY', 'PRODUCT_SOLUTION_ENGINEERING'], supportTypes: ['ADVISORY_ASSESSMENT', 'INTEGRATION'] }));
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