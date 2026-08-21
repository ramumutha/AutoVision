import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { ConnectivityService } from '../../core/connectivity/connectivity.service';
import { OperationalAuthorizationApiService } from './operational-authorization-api.service';
import { OperationalAuthorizationEvaluation } from './operational-authorization.models';
import { ServiceJobOperationalAuthorizationComponent } from './service-job-operational-authorization.component';
import { ServiceJobAuthorizationReadinessApiService } from './service-job-authorization-readiness-api.service';
import { ServiceJobAuthorizationReadiness } from './service-job-authorization-readiness.models';

interface EvaluationApiMock { evaluate: ReturnType<typeof vi.fn>; }

describe('ServiceJobOperationalAuthorizationComponent', () => {
  let fixture: ComponentFixture<ServiceJobOperationalAuthorizationComponent>;
  let api: EvaluationApiMock;
  let readinessApi: { evaluate: ReturnType<typeof vi.fn> };
  let connectivity: { isOnline: ReturnType<typeof vi.fn> };
  const jobs = [{ id: 'job-1', jobNumber: 'JOB-001', summary: 'Brake work' }];
  const evaluation: OperationalAuthorizationEvaluation = {
    serviceJobId: 'job-1', serviceOrderId: 'order-1', status: 'FULLY_AUTHORIZED',
    totalLineCount: 2, authorizedLineCount: 2, pendingLineCount: 0, notAuthorizedLineCount: 0,
  };

  async function create(result = of(evaluation), readinessApiMock?: { evaluate: ReturnType<typeof vi.fn> }): Promise<void> {
    api = { evaluate: vi.fn().mockReturnValue(result) };
    readinessApi = readinessApiMock ?? { evaluate: vi.fn().mockReturnValue(of({ serviceJobId: 'job-1', serviceOrderId: 'order-1', ready: true, operationalAuthorizationStatus: 'FULLY_AUTHORIZED', reason: 'FULLY_AUTHORIZED' } satisfies ServiceJobAuthorizationReadiness)) };
    connectivity = { isOnline: vi.fn().mockReturnValue(true) };
    await TestBed.configureTestingModule({
      imports: [ServiceJobOperationalAuthorizationComponent],
      providers: [
        { provide: OperationalAuthorizationApiService, useValue: api },
        { provide: ServiceJobAuthorizationReadinessApiService, useValue: readinessApi },
        { provide: ConnectivityService, useValue: connectivity },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ServiceJobOperationalAuthorizationComponent);
    fixture.componentRef.setInput('orderId', 'order-1');
    fixture.componentRef.setInput('jobs', jobs);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  it.each([
    ['FULLY_AUTHORIZED', 'success'], ['PARTIALLY_AUTHORIZED', 'warning'], ['PENDING', 'warning'],
    ['NOT_AUTHORIZED', 'danger'], ['NOT_REQUIRED', 'neutral'],
  ] as const)('renders %s with %s tone', async (status, tone) => {
    await create(of({ ...evaluation, status }));
    const statusElement = fixture.nativeElement.querySelector('.status') as HTMLElement;
    expect(statusElement.className).toContain(tone);
  });

  it('renders backend counts for required authorization results', async () => {
    await create(of({ ...evaluation, status: 'PARTIALLY_AUTHORIZED', pendingLineCount: 1, authorizedLineCount: 1 }));
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Total Job Lines');
    expect(text).toContain('Authorized Lines');
    expect(text).toContain('Pending Lines');
    expect(text).toContain('Not Authorized Lines');
    expect(text).toContain('2');
  });

  it('presents NOT_REQUIRED without authorization evidence counts', async () => {
    await create(of({ ...evaluation, status: 'NOT_REQUIRED', authorizedLineCount: 0 }));
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Authorization is not required for this job.');
    expect(text).toContain('Total Job Lines');
    expect(text).not.toContain('Authorized Lines');
  });

  it('keeps job content visible during evaluation loading', async () => {
    await create(new Subject<OperationalAuthorizationEvaluation>());
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('JOB-001');
    expect(text).toContain('Loading authorization evaluation');
  });

  it('keeps job content visible on error and retries only evaluation', async () => {
    const first = new Subject<OperationalAuthorizationEvaluation>();
    await create(first);
    first.error({ status: 500, message: 'Evaluation failed' });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Unable to load authorization evaluation');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('JOB-001');
    api.evaluate.mockReturnValue(of(evaluation));
    (fixture.nativeElement.querySelector('.retry') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(api.evaluate).toHaveBeenCalledTimes(2);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('FULLY AUTHORIZED');
  });

  it('uses backend readiness even when it conflicts with evaluation status', async () => {
    await create(of({ ...evaluation, status: 'NOT_AUTHORIZED' }), { evaluate: vi.fn().mockReturnValue(of({ serviceJobId: 'job-1', serviceOrderId: 'order-1', ready: true, operationalAuthorizationStatus: 'NOT_AUTHORIZED', reason: 'AUTHORIZATION_MISSING' } satisfies ServiceJobAuthorizationReadiness)) });
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Ready');
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Blocked');
  });

  it.each([
    'AUTHORIZATION_NOT_REQUIRED', 'FULLY_AUTHORIZED', 'PARTIAL_AUTHORIZATION',
    'AUTHORIZATION_PENDING', 'AUTHORIZATION_MISSING',
  ] as const)('renders readiness reason %s from the backend', async (reason) => {
    await create(of(evaluation), { evaluate: vi.fn().mockReturnValue(of({
      serviceJobId: 'job-1', serviceOrderId: 'order-1', ready: false,
      operationalAuthorizationStatus: 'PENDING', reason,
    } satisfies ServiceJobAuthorizationReadiness)) });
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      ({
        AUTHORIZATION_NOT_REQUIRED: 'Authorization not required',
        FULLY_AUTHORIZED: 'Fully authorized',
        PARTIAL_AUTHORIZATION: 'Partial authorization',
        AUTHORIZATION_PENDING: 'Authorization pending',
        AUTHORIZATION_MISSING: 'Authorization missing',
      } as const)[reason],
    );
  });

  it('keeps evaluation visible when readiness fails and retries readiness only', async () => {
    const readiness = new Subject<ServiceJobAuthorizationReadiness>();
    await create(of(evaluation), { evaluate: vi.fn().mockReturnValue(readiness) });
    readiness.error({ status: 500, message: 'Readiness failed' });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('FULLY AUTHORIZED');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Unable to load authorization readiness');
    expect(api.evaluate).toHaveBeenCalledTimes(1);
    connectivity.isOnline.mockReturnValue(false);
    readinessApi.evaluate.mockReturnValue(throwError(() => ({ status: 0, message: 'Offline' })));
    (fixture.nativeElement.querySelector('.readiness-retry') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(readinessApi.evaluate).toHaveBeenCalledTimes(2);
    expect(api.evaluate).toHaveBeenCalledTimes(1);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('You appear to be offline');
  });
});
