import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ServiceProfitApiService } from './service-profit-api.service';
import { ServiceProfitOpportunityWorkspaceComponent } from './service-profit-opportunity-workspace.component';
import { ServiceProfitOpportunityResponse } from './service-profit.models';

describe('ServiceProfitOpportunityWorkspaceComponent', () => {
  let fixture: ComponentFixture<ServiceProfitOpportunityWorkspaceComponent>;
  const api = {
    getOpportunityFollowUp: vi.fn(),
    getOpportunityFollowUpHistory: vi.fn(),
  };

  const opportunity = {
    id: 'opportunity-1', opportunityType: 'DECLINED_WORK', status: 'DETECTED', evidenceClass: 'SOURCE_CONFIRMED', evidenceStrength: 'STRONG',
    priority: 'HIGH', actionability: 'READY', title: 'Recover declined brake work', summary: 'A declined brake recommendation may be recoverable.',
    potentialAmount: 320, currencyCode: 'USD', detectedAt: '2026-08-20T10:00:00Z', sourceSystem: 'DMS', sourceEntityType: 'SERVICE_LINE', sourceEntityId: 'line-1',
    sourceServiceOrderId: null, sourceServiceJobId: null, sourceServiceLineId: 'line-1', sourceQuoteId: null, policyVersion: 'service-profit-r1',
    suppressionReason: null, suppressedAt: null, customerId: 'customer-secret', vehicleId: 'vehicle-secret', version: 1, createdAt: '2026-08-20T10:00:00Z', updatedAt: '2026-08-20T10:00:00Z',
    explanation: { headline: 'Declined work found', rationale: 'A source record confirms it.', evidenceBasis: 'Service history.', recommendedAction: 'Contact the customer.' }, context: null,
  } as ServiceProfitOpportunityResponse;

  beforeEach(async () => {
    api.getOpportunityFollowUp.mockReset();
    api.getOpportunityFollowUpHistory.mockReset();
    await TestBed.configureTestingModule({
      imports: [ServiceProfitOpportunityWorkspaceComponent],
      providers: [{ provide: ServiceProfitApiService, useValue: api }],
    }).compileComponents();
    fixture = TestBed.createComponent(ServiceProfitOpportunityWorkspaceComponent);
    fixture.componentRef.setInput('opportunity', opportunity);
    fixture.detectChanges();
  });

  it('renders authoritative overview fields without exposing internal identifiers', () => {
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Recover declined brake work');
    expect(text).toContain('Declined work found');
    expect(text).toContain('A source record confirms it.');
    expect(text).not.toContain('customer-secret');
    expect(text).not.toContain('vehicle-secret');
    expect(text).not.toContain('Contact the customer.');
    expect(fixture.nativeElement.querySelector('button:not([role="tab"])')).toBeNull();
  });

  it('shows supported evidence and loads follow-up and history independently', () => {
    api.getOpportunityFollowUp.mockReturnValue(of({ opportunityId: 'opportunity-1', handlingStatus: 'OPEN', ownership: 'MINE', dueAt: '2026-08-29T10:00:00Z', dueState: 'UPCOMING', disposition: 'FOLLOW_UP_REQUIRED', version: 3 }));
    api.getOpportunityFollowUpHistory.mockReturnValue(of([
      { eventType: 'CREATED', actorType: 'SYSTEM', actorIdentity: 'AUTOVISION_SERVICE_PROFIT', applicationIdentity: 'AUTOVISION_SERVICE_PROFIT', occurredAt: '2026-08-27T10:10:00Z', previousValue: null, newValue: null },
      { eventType: 'OWNERSHIP_CLAIMED', actorType: 'HUMAN', actorIdentity: 'ME', applicationIdentity: null, occurredAt: '2026-08-27T10:15:00Z', previousValue: null, newValue: 'IN_PROGRESS' },
    ]));
    const tabs = (fixture.nativeElement as HTMLElement).querySelectorAll('[role="tab"]');
    (tabs[1] as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Service history.');
    expect(fixture.nativeElement.textContent).toContain('Detailed evidence events are not available');

    (tabs[2] as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(api.getOpportunityFollowUp).toHaveBeenCalledWith('opportunity-1');
    expect(fixture.nativeElement.textContent).toContain('Follow Up Required');
    expect(fixture.nativeElement.textContent).toContain('Mine');
    expect(fixture.nativeElement.textContent).not.toContain('principal');

    (tabs[3] as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(api.getOpportunityFollowUpHistory).toHaveBeenCalledWith('opportunity-1');
    expect(fixture.nativeElement.textContent).toContain('Created');
    expect(fixture.nativeElement.textContent).toContain('AutoVision Service Profit');
    expect(fixture.nativeElement.textContent).toContain('Me');
  });

  it('renders a missing follow-up as an intentional empty state', () => {
    api.getOpportunityFollowUp.mockReturnValue(throwError(() => ({ status: 404 })));
    const tabs = fixture.nativeElement.querySelectorAll('[role="tab"]');
    (tabs[2] as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No follow-up has been created');
    expect(fixture.nativeElement.textContent).not.toContain('Unable to load follow-up');
  });

  it('keeps the overview available when follow-up fails and retries', () => {
    api.getOpportunityFollowUp.mockReturnValueOnce(throwError(() => ({ status: 503 }))).mockReturnValueOnce(of({ opportunityId: 'opportunity-1', handlingStatus: 'COMPLETED', ownership: 'UNASSIGNED', dueAt: null, dueState: 'NO_DUE_DATE', disposition: 'NONE', version: 4 }));
    const tabs = fixture.nativeElement.querySelectorAll('[role="tab"]');
    (tabs[2] as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Unable to load follow-up');
    expect(fixture.nativeElement.textContent).toContain('Recover declined brake work');
    (fixture.nativeElement.querySelector('.retry') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Unassigned');
  });

  it('renders empty history and keeps evidence available on history failure', () => {
    api.getOpportunityFollowUpHistory.mockReturnValueOnce(of([]));
    const tabs = fixture.nativeElement.querySelectorAll('[role="tab"]');
    (tabs[3] as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No operational history recorded yet');
    (tabs[1] as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Service history.');
  });
});