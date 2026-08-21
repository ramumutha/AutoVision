import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { ConnectivityService } from '../../core/connectivity/connectivity.service';
import { CustomerAuthorizationApiService } from './customer-authorization-api.service';
import { CustomerAuthorization } from './customer-authorization.models';
import { CustomerAuthorizationDecisionAction, ServiceQuoteAuthorizationDecisionComponent } from './service-quote-authorization-decision.component';

describe('ServiceQuoteAuthorizationDecisionComponent', () => {
  let fixture: ComponentFixture<ServiceQuoteAuthorizationDecisionComponent>;
  type AuthorizationDecisionApiMock = {
    authorize: ReturnType<typeof vi.fn>;
    decline: ReturnType<typeof vi.fn>;
    defer: ReturnType<typeof vi.fn>;
    cancel: ReturnType<typeof vi.fn>;
  };
  let api: AuthorizationDecisionApiMock;
  let connectivity: { isOnline: ReturnType<typeof vi.fn> };
  const authorization: CustomerAuthorization = {
    id: 'authorization-1', tenantId: 'tenant-1', dealerId: null, branchId: null, aftersalesCaseId: 'case-1', serviceQuoteId: 'quote-1',
    authorizationNumber: 'AUTH-1', authorizationStatus: 'REQUESTED', customerReference: null, customerDisplayNameSnapshot: null,
    authorizationSummary: 'Authorize quoted work', authorizationScopeSnapshot: {}, commercialSnapshot: null, termsSnapshot: null, disclaimerSnapshot: null,
    requestedAt: '2026-08-21T10:00:00Z', decidedAt: null, decisionChannel: null, decisionReference: null, version: 0,
    createdByPrincipalId: null, updatedByPrincipalId: null, createdAt: '2026-08-21T10:00:00Z', updatedAt: '2026-08-21T10:00:00Z',
  };

  async function create(action: CustomerAuthorizationDecisionAction, result = of(authorization)): Promise<void> {
    api = {
      authorize: vi.fn().mockReturnValue(of(authorization)),
      decline: vi.fn().mockReturnValue(of(authorization)),
      defer: vi.fn().mockReturnValue(of(authorization)),
      cancel: vi.fn().mockReturnValue(of(authorization)),
    };
    api[action].mockReturnValue(result);
    connectivity = { isOnline: vi.fn().mockReturnValue(true) };
    await TestBed.configureTestingModule({
      imports: [ServiceQuoteAuthorizationDecisionComponent],
      providers: [
        { provide: CustomerAuthorizationApiService, useValue: api },
        { provide: ConnectivityService, useValue: connectivity },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ServiceQuoteAuthorizationDecisionComponent);
    fixture.componentRef.setInput('caseId', 'case/1');
    fixture.componentRef.setInput('authorization', authorization);
    fixture.componentRef.setInput('action', action);
    fixture.detectChanges();
  }

  function fillFields(): void {
    fixture.componentInstance.form.setValue({ decisionChannel: ' EMAIL ', decisionReference: ' DEC-1 ' });
  }

  it.each(['authorize', 'decline', 'defer', 'cancel'] as CustomerAuthorizationDecisionAction[])(
    'dispatches the %s API method with the exact bounded trimmed body', async (action) => {
      await create(action);
      fillFields();
      fixture.componentInstance.submit();
      expect(api[action]).toHaveBeenCalledWith('case/1', 'authorization-1', { decisionChannel: 'EMAIL', decisionReference: 'DEC-1' });
      expect(Object.keys(api[action].mock.calls[0][2])).toEqual(['decisionChannel', 'decisionReference']);
    },
  );

  it('converts blank decision fields to null and emits the response', async () => {
    await create('authorize');
    const emitted: CustomerAuthorization[] = [];
    fixture.componentInstance.decided.subscribe((value) => emitted.push(value));
    fixture.componentInstance.form.setValue({ decisionChannel: '  ', decisionReference: '' });
    fixture.componentInstance.submit();
    expect(api.authorize).toHaveBeenCalledWith('case/1', 'authorization-1', { decisionChannel: null, decisionReference: null });
    expect(emitted).toEqual([authorization]);
  });

  it('prevents duplicate submission while pending', async () => {
    const pending = new Subject<CustomerAuthorization>();
    await create('defer', pending);
    fillFields();
    fixture.componentInstance.submit();
    fixture.componentInstance.submit();
    expect(api.defer).toHaveBeenCalledTimes(1);
    pending.next(authorization);
    pending.complete();
  });

  it('keeps offline and HTTP errors local', async () => {
    await create('cancel', new Subject<CustomerAuthorization>());
    fillFields();
    connectivity.isOnline.mockReturnValue(false);
    fixture.componentInstance.submit();
    fixture.detectChanges();
    expect(api.cancel).not.toHaveBeenCalled();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('requires a connection');
  });

  it.each([
    [403, 'You are not authorized'],
    [404, 'Authorization decision context unavailable'],
    [409, 'Decision conflict'],
    [500, 'Something went wrong'],
    [400, 'Decision failed'],
  ])('renders local error handling for status %s', async (status, message) => {
    const failed = new Subject<CustomerAuthorization>();
    await create('decline', failed);
    fillFields();
    fixture.componentInstance.submit();
    failed.error({ status, message });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      status === 403 ? 'You are not authorized' : status === 404 ? 'Authorization decision context unavailable' : status === 409 ? 'Decision conflict' : status >= 500 ? 'Something went wrong' : 'Decision failed',
    );
  });
});
