import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { ConnectivityService } from '../../core/connectivity/connectivity.service';
import { CustomerAuthorizationApiService } from './customer-authorization-api.service';
import { CustomerAuthorization } from './customer-authorization.models';
import { ServiceQuoteAuthorizationRequestComponent } from './service-quote-authorization-request.component';

describe('ServiceQuoteAuthorizationRequestComponent', () => {
  let fixture: ComponentFixture<ServiceQuoteAuthorizationRequestComponent>;
  let api: { requestFromServiceQuote: ReturnType<typeof vi.fn> };
  let connectivity: { isOnline: ReturnType<typeof vi.fn> };
  const authorization: CustomerAuthorization = {
    id: 'authorization-1', tenantId: 'tenant-1', dealerId: null, branchId: null,
    aftersalesCaseId: 'case-1', serviceQuoteId: 'quote-1', authorizationNumber: 'AUTH-1',
    authorizationStatus: 'REQUESTED', customerReference: null, customerDisplayNameSnapshot: null,
    authorizationSummary: 'Authorize quoted work', authorizationScopeSnapshot: {}, commercialSnapshot: null,
    termsSnapshot: null, disclaimerSnapshot: null, requestedAt: '2026-08-21T10:00:00Z',
    decidedAt: null, decisionChannel: null, decisionReference: null, version: 0,
    createdByPrincipalId: 'principal-1', updatedByPrincipalId: 'principal-1',
    createdAt: '2026-08-21T10:00:00Z', updatedAt: '2026-08-21T10:00:00Z',
  };

  async function create(): Promise<void> {
    api = { requestFromServiceQuote: vi.fn().mockReturnValue(of(authorization)) };
    connectivity = { isOnline: vi.fn().mockReturnValue(true) };
    await TestBed.configureTestingModule({
      imports: [ServiceQuoteAuthorizationRequestComponent],
      providers: [
        { provide: CustomerAuthorizationApiService, useValue: api },
        { provide: ConnectivityService, useValue: connectivity },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ServiceQuoteAuthorizationRequestComponent);
    fixture.componentRef.setInput('caseId', 'case/1');
    fixture.componentRef.setInput('quoteId', 'quote/1');
    fixture.detectChanges();
  }

  function fillRequired(): void {
    const component = fixture.componentInstance;
    component.form.controls.authorizationNumber.setValue(' AUTH-1 ');
    component.form.controls.authorizationSummary.setValue(' Authorize quoted work ');
    component.form.controls.customerReference.setValue('   ');
    component.form.controls.customerDisplayNameSnapshot.setValue('   ');
  }

  it('enforces required and maximum-length fields', async () => {
    await create();
    const component = fixture.componentInstance;
    expect(component.form.invalid).toBe(true);
    component.form.controls.authorizationNumber.setValue('A'.repeat(81));
    component.form.controls.authorizationSummary.setValue('Summary');
    expect(component.form.invalid).toBe(true);
    component.form.controls.authorizationNumber.setValue('AUTH-1');
    component.form.controls.authorizationSummary.setValue('S'.repeat(501));
    expect(component.form.invalid).toBe(true);
  });

  it('sends the exact bounded request with trimmed and null optional values', async () => {
    await create();
    fillRequired();
    fixture.componentInstance.submit();

    expect(api.requestFromServiceQuote).toHaveBeenCalledWith('case/1', 'quote/1', {
      authorizationNumber: 'AUTH-1',
      customerReference: null,
      customerDisplayNameSnapshot: null,
      authorizationSummary: 'Authorize quoted work',
    });
  });

  it('prevents duplicate submission while the request is pending', async () => {
    await create();
    const pending = new Subject<CustomerAuthorization>();
    api.requestFromServiceQuote.mockReturnValue(pending);
    fillRequired();
    fixture.componentInstance.submit();
    fixture.componentInstance.submit();
    expect(api.requestFromServiceQuote).toHaveBeenCalledTimes(1);
    pending.next(authorization);
    pending.complete();
  });

  it('emits the returned authorization on success', async () => {
    await create();
    const requested: CustomerAuthorization[] = [];
    fixture.componentInstance.requested.subscribe((value) => requested.push(value));
    fillRequired();
    fixture.componentInstance.submit();
    expect(requested).toEqual([authorization]);
  });

  it.each([
    [403, 'not authorized'],
    [409, 'conflict'],
  ])('keeps HTTP %s errors local', async (status) => {
    await create();
    api.requestFromServiceQuote.mockReturnValue(new Subject<CustomerAuthorization>());
    fillRequired();
    fixture.componentInstance.submit();
    const request = api.requestFromServiceQuote.mock.results[0].value as Subject<CustomerAuthorization>;
    request.error({ status, message: status === 409 ? 'Conflict' : 'Forbidden' });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(status === 409 ? 'Conflict' : 'You are not authorized');
  });

  it('reports offline state without calling the API', async () => {
    await create();
    connectivity.isOnline.mockReturnValue(false);
    fillRequired();
    fixture.componentInstance.submit();
    fixture.detectChanges();
    expect(api.requestFromServiceQuote).not.toHaveBeenCalled();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('requires a connection');
  });
});
