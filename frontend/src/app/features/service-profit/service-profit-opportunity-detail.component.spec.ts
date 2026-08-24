import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ServiceProfitOpportunityDetailComponent } from './service-profit-opportunity-detail.component';
import { ServiceProfitOpportunityResponse } from './service-profit.models';

describe('ServiceProfitOpportunityDetailComponent', () => {
  let fixture: ComponentFixture<ServiceProfitOpportunityDetailComponent>;

  const opportunity: ServiceProfitOpportunityResponse = {
    id: 'opportunity-1', tenantId: 'tenant-1', dealerId: 'dealer-1', branchId: 'branch-1', locationId: 'location-1',
    opportunityKey: 'declined-1', opportunityType: 'DECLINED_WORK', status: 'SUPPRESSED', evidenceClass: 'SOURCE_CONFIRMED',
    evidenceStrength: 'STRONG', priority: 'HIGH', actionability: 'SUPPRESSED', title: 'Recover declined brake work',
    potentialAmount: 320, currencyCode: 'USD', detectedAt: '2026-08-20T10:00:00Z', customerId: 'hidden-customer-id',
    vehicleId: 'hidden-vehicle-id', summary: 'Brake work was declined.', sourceSystem: 'DMS', sourceEntityType: 'SERVICE_LINE',
    sourceEntityId: 'line-1', sourceServiceOrderId: 'order-1', sourceServiceJobId: null, sourceServiceLineId: 'line-1',
    sourceQuoteId: null, policyVersion: 'service-profit-r1', suppressionReason: 'WORK_ALREADY_COMPLETED',
    suppressedAt: '2026-08-21T10:00:00Z', explanation: {
      headline: 'Previously declined work was identified',
      rationale: 'The source record shows a customer decline.',
      evidenceBasis: 'Confirmed service line history.',
      recommendedAction: 'Do not contact the customer while suppressed.',
    },
    version: 1, createdAt: '2026-08-20T10:00:00Z', updatedAt: '2026-08-21T10:00:00Z',
    context: {
      customer: { displayName: 'Arjun Mehta', reference: 'CUST-001', phone: '+919900000001', email: 'arjun.mehta@example.demo', contactable: true },
      vehicle: { registration: 'KA01AV1001', vin: 'VINDEMO00000000001', make: 'Demo Motors', model: 'City Prime', modelYear: 2022, powertrain: 'ICE' },
      service: { orderReference: 'RO-1001', serviceDate: '2026-07-01', description: 'Front brake pad replacement', advisorContext: 'Customer declined front brake work today.' },
    },
  };

  async function render(value: ServiceProfitOpportunityResponse = opportunity): Promise<HTMLElement> {
    await TestBed.configureTestingModule({ imports: [ServiceProfitOpportunityDetailComponent] }).compileComponents();
    fixture = TestBed.createComponent(ServiceProfitOpportunityDetailComponent);
    fixture.componentRef.setInput('opportunity', value);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('renders customer context without exposing its raw identifier', async () => {
    const element = await render();
    expect(element.querySelector('.customer-context')?.textContent).toContain('Arjun Mehta');
    expect(element.textContent).not.toContain('hidden-customer-id');
  });

  it('renders vehicle context without exposing its raw identifier', async () => {
    const element = await render();
    expect(element.querySelector('.vehicle-context')?.textContent).toContain('KA01AV1001');
    expect(element.querySelector('.vehicle-context')?.textContent).toContain('Demo Motors City Prime · 2022');
    expect(element.textContent).not.toContain('hidden-vehicle-id');
  });

  it('renders service context', async () => {
    const element = await render();
    expect(element.querySelector('.service-context')?.textContent).toContain('RO-1001');
    expect(element.querySelector('.service-context')?.textContent).toContain('Front brake pad replacement');
  });

  it('renders recoverable commercial value with its currency', async () => {
    const element = await render();
    expect(element.querySelector('.commercial-summary')?.textContent).toContain('$320');
  });

  it('renders the explanation and evidence basis', async () => {
    const element = await render();
    expect(element.querySelector('.explanation')?.textContent).toContain('Previously declined work was identified');
    expect(element.querySelector('.explanation')?.textContent).toContain('Confirmed service line history.');
  });

  it('renders the recommended action', async () => {
    const element = await render();
    expect(element.querySelector('.recommended-action')?.textContent).toContain('Do not contact the customer while suppressed.');
  });

  it('renders the suppressed DO NOT ACTION safeguard', async () => {
    const element = await render();
    expect(element.querySelector('.suppression')?.textContent).toContain('Do not action');
    expect(element.querySelector('.suppression')?.textContent).toContain('Work Already Completed');
  });

  it('renders REVIEW_REQUIRED guidance before customer contact', async () => {
    const element = await render({ ...opportunity, status: 'DETECTED', actionability: 'REVIEW_REQUIRED', suppressionReason: null, suppressedAt: null });
    expect(element.querySelector('.review-required')?.textContent).toContain('Review is required before customer contact.');
  });

  it('keeps provenance available through a collapsed native disclosure', async () => {
    const element = await render();
    const audit = element.querySelector('details.audit-details');
    expect(audit?.querySelector('summary')?.textContent).toContain('Evidence & audit details');
    expect(audit?.textContent).toContain('line-1');
    expect(audit?.hasAttribute('open')).toBe(false);
  });

  it('handles missing context without rendering empty context sections', async () => {
    const element = await render({ ...opportunity, context: null });
    expect(element.querySelector('.context-grid')).toBeNull();
    expect(element.querySelector('.explanation')?.textContent).toContain('Previously declined work was identified');
  });
});
