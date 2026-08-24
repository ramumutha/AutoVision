import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ServiceProfitDataCapabilityComponent } from './service-profit-data-capability.component';
import { ServiceProfitDataCapabilityResponse } from './service-profit.models';

describe('ServiceProfitDataCapabilityComponent', () => {
  let fixture: ComponentFixture<ServiceProfitDataCapabilityComponent>;

  const assessed: ServiceProfitDataCapabilityResponse = {
    assessmentState: 'ASSESSED',
    sourceDatasetId: 'AUTOVISION-SERVICE-PROFIT-R1-DEMO',
    sourceDatasetVersion: '1.0.0',
    assessmentPolicyVersion: 'service-profit-data-readiness-r1',
    assessedAt: '2026-08-24T05:00:00Z',
    capabilities: [
      {
        capability: 'REVENUE_ATTRIBUTION',
        status: 'AVAILABLE',
        reason: 'Invoice linkage supports revenue attribution.',
      },
      {
        capability: 'GROSS_PROFIT_ATTRIBUTION',
        status: 'PARTIAL',
        reason: 'Cost data is available for only part of the assessed population.',
      },
    ],
  };

  async function createComponent(
    data: ServiceProfitDataCapabilityResponse = assessed,
  ): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [ServiceProfitDataCapabilityComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ServiceProfitDataCapabilityComponent);
    fixture.componentRef.setInput('view', { state: 'available', data });
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  it('keeps the commercial capability summary compact without exposing raw assessment scores', async () => {
    await createComponent();

    const element = fixture.nativeElement as HTMLElement;
    const text = element.textContent ?? '';

    expect(element.querySelector('h2')).toBeNull();
    expect(text).not.toContain('Data capability');
    expect(element.querySelectorAll('av-status')).toHaveLength(0);

    expect(text).not.toContain('overallScore');
    expect(text).not.toContain('identityCoverage');
    expect(text).not.toContain('invoiceLinkageCoverage');
    expect(text).not.toContain('costCoverage');
    expect(text).not.toContain('%');
  });

  it('keeps technical assessment details collapsed until explicitly requested', async () => {
    await createComponent();

    const element = fixture.nativeElement as HTMLElement;
    const toggle = element.querySelector<HTMLButtonElement>('.details-toggle');

    expect(toggle?.getAttribute('aria-expanded')).toBe('false');
    expect(toggle?.getAttribute('aria-controls')).toBe(
      'service-profit-data-capability-details',
    );

    expect(element.textContent).not.toContain('AUTOVISION-SERVICE-PROFIT-R1-DEMO');
    expect(element.textContent).not.toContain('Invoice linkage supports revenue attribution.');

    toggle?.click();
    fixture.detectChanges();

    expect(toggle?.getAttribute('aria-expanded')).toBe('true');
    expect(
      element.querySelector('#service-profit-data-capability-details'),
    ).not.toBeNull();

    const text = element.textContent ?? '';
    expect(text).toContain('AUTOVISION-SERVICE-PROFIT-R1-DEMO');
    expect(text).toContain('1.0.0');
    expect(text).toContain('service-profit-data-readiness-r1');
    expect(text).toContain('Invoice linkage supports revenue attribution.');
    expect(text).toContain(
      'Cost data is available for only part of the assessed population.',
    );
  });

  it('represents unavailable capability with visible text rather than color alone', async () => {
    await createComponent({
      ...assessed,
      capabilities: [
        {
          capability: 'REVENUE_ATTRIBUTION',
          status: 'UNAVAILABLE',
          reason: 'Invoice linkage is unavailable.',
        },
        {
          capability: 'GROSS_PROFIT_ATTRIBUTION',
          status: 'UNAVAILABLE',
          reason: 'Cost data is unavailable.',
        },
      ],
    });

    const element = fixture.nativeElement as HTMLElement;
    const text = element.textContent ?? '';

    expect(element.querySelectorAll('av-status')).toHaveLength(0);

    element.querySelector<HTMLButtonElement>('.details-toggle')?.click();
    fixture.detectChanges();
    const expandedText = element.textContent ?? '';
    expect(expandedText).toContain('Revenue data');
    expect(expandedText).toContain('Gross profit data');
    expect(Array.from(element.querySelectorAll('av-status')).map((status) => status.textContent?.trim()))
      .toEqual(['Unavailable', 'Unavailable']);
  });

  it('clearly represents a dealer whose data has not yet been assessed', async () => {
    await createComponent({
      assessmentState: 'NOT_ASSESSED',
      sourceDatasetId: null,
      sourceDatasetVersion: null,
      assessmentPolicyVersion: null,
      assessedAt: null,
      capabilities: [],
    });

    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).not.toContain('Data capability not yet assessed');
    expect(element.textContent).not.toContain('Revenue data');
    expect(element.textContent).not.toContain('Gross profit data');

    element.querySelector<HTMLButtonElement>('.details-toggle')?.click();
    fixture.detectChanges();

    expect(element.textContent).toContain(
      'Commercial attribution capability will be shown after dealer data assessment is completed.',
    );
  });

  it('supports accessible expand and collapse disclosure semantics', async () => {
    await createComponent();

    const element = fixture.nativeElement as HTMLElement;
    const toggle = element.querySelector<HTMLButtonElement>('.details-toggle');

    expect(toggle?.tagName).toBe('BUTTON');
    expect(toggle?.type).toBe('button');
    expect(toggle?.textContent?.trim()).toBe('');
    expect(toggle?.getAttribute('aria-label')).toBe('View details');

    toggle?.click();
    fixture.detectChanges();

    expect(toggle?.getAttribute('aria-expanded')).toBe('true');
    expect(toggle?.textContent?.trim()).toBe('');

    element.querySelector<HTMLButtonElement>('.close-details')?.click();
    fixture.detectChanges();

    expect(toggle?.getAttribute('aria-expanded')).toBe('false');
    expect(
      element.querySelector('#service-profit-data-capability-details'),
    ).toBeNull();
  });

  it('keeps loading and API failure distinct from assessed capability statuses', async () => {
    await TestBed.configureTestingModule({
      imports: [ServiceProfitDataCapabilityComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ServiceProfitDataCapabilityComponent);
    fixture.componentRef.setInput('view', { state: 'loading' });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('Checking data capability');
    expect(fixture.nativeElement.querySelector('.details-toggle')).not.toBeNull();
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.details-toggle')?.click();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Checking data capability');

    fixture.componentRef.setInput('view', { state: 'failed' });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Data capability unavailable');
    expect(fixture.nativeElement.textContent).not.toContain('UnavailableUnavailable');
    expect(fixture.nativeElement.querySelector('av-status')).toBeNull();
  });
});