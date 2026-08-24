import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of, Subject, throwError } from 'rxjs';
import { authenticatedGuard } from '../../core/auth/auth.guard';
import { routes } from '../../app.routes';
import { ServiceProfitApiService } from './service-profit-api.service';
import { ServiceProfitOpportunityDetailPageComponent } from './service-profit-opportunity-detail-page.component';
import { ServiceProfitOpportunityResponse } from './service-profit.models';

@Component({ template: '' })
class EmptyComponent {}

describe('ServiceProfitOpportunityDetailPageComponent', () => {
  const opportunity = {
    id: 'opportunity-1', opportunityType: 'DECLINED_WORK', status: 'DETECTED', evidenceStrength: 'STRONG',
    priority: 'HIGH', actionability: 'READY', title: 'Recover declined brake work', potentialAmount: 320,
    currencyCode: 'USD', detectedAt: '2026-08-20T10:00:00Z', sourceSystem: 'DMS', sourceEntityType: 'SERVICE_LINE',
    sourceEntityId: 'line-1', policyVersion: 'service-profit-r1', suppressionReason: null, suppressedAt: null,
    explanation: { headline: 'Declined work found', rationale: 'A source record confirms it.', evidenceBasis: 'Service history.', recommendedAction: 'Contact the customer.' },
    context: null,
  } as ServiceProfitOpportunityResponse;

  const api = { getOpportunity: vi.fn() };

  beforeEach(() => {
    api.getOpportunity.mockReset();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: 'service-profit/opportunities/:opportunityId', component: ServiceProfitOpportunityDetailPageComponent },
          { path: 'service-profit', component: EmptyComponent },
        ]),
        { provide: ServiceProfitApiService, useValue: api },
      ],
    });
  });

  it('keeps the application detail route guarded', () => {
    const detailRoute = routes.find((route) => route.path === 'service-profit/opportunities/:opportunityId');
    expect(detailRoute?.canActivate).toContain(authenticatedGuard);
  });

  it('loads a direct valid ID and reuses the authoritative detail presentation', async () => {
    api.getOpportunity.mockReturnValue(of(opportunity));
    const harness = await RouterTestingHarness.create();
    const component = await harness.navigateByUrl('/service-profit/opportunities/opportunity-1', ServiceProfitOpportunityDetailPageComponent);
    harness.detectChanges();
    expect(api.getOpportunity).toHaveBeenCalledWith('opportunity-1');
    expect(component).toBeInstanceOf(ServiceProfitOpportunityDetailPageComponent);
    expect(harness.routeNativeElement?.querySelector('app-service-profit-opportunity-detail')).not.toBeNull();
    expect(harness.routeNativeElement?.textContent).toContain('Recover declined brake work');
  });

  it('shows a localized unavailable state for a missing opportunity', async () => {
    api.getOpportunity.mockReturnValue(throwError(() => ({ status: 404, message: 'missing' })));
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/service-profit/opportunities/missing', ServiceProfitOpportunityDetailPageComponent);
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Opportunity unavailable');
    expect(harness.routeNativeElement?.textContent).toContain('could not be found');
    expect(harness.routeNativeElement?.querySelector('.retry-action')).toBeNull();
  });

  it('preserves list query state when a direct-link Back needs the list fallback', async () => {
    api.getOpportunity.mockReturnValue(of(opportunity));
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/service-profit/opportunities/opportunity-1?type=DECLINED_WORK&priority=HIGH&actionability=READY&sort=POTENTIAL_DESC', ServiceProfitOpportunityDetailPageComponent);
    harness.detectChanges();
    history.replaceState({ navigationId: 1 }, '');
    harness.routeNativeElement?.querySelector<HTMLButtonElement>('.back-action')?.click();
    await harness.fixture.whenStable();
    expect(TestBed.inject(Router).url).toBe('/service-profit?type=DECLINED_WORK&priority=HIGH&actionability=READY&sort=POTENTIAL_DESC');
  });

  it('cancels stale detail requests when the route ID changes', async () => {
    const first = new Subject<ServiceProfitOpportunityResponse>();
    const second = new Subject<ServiceProfitOpportunityResponse>();
    api.getOpportunity.mockImplementation((id: string) => id === 'first' ? first : second);
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/service-profit/opportunities/first', ServiceProfitOpportunityDetailPageComponent);
    await harness.navigateByUrl('/service-profit/opportunities/second', ServiceProfitOpportunityDetailPageComponent);
    first.next({ ...opportunity, id: 'first', title: 'Stale opportunity' });
    second.next({ ...opportunity, id: 'second', title: 'Current opportunity' });
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Current opportunity');
    expect(harness.routeNativeElement?.textContent).not.toContain('Stale opportunity');
  });
});
