import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { ConnectivityService } from '../../core/connectivity/connectivity.service';
import { OperationalAuthorizationApiService } from './operational-authorization-api.service';
import { OperationalAuthorizationEvaluation } from './operational-authorization.models';
import { ServiceJobOperationalAuthorizationComponent } from './service-job-operational-authorization.component';

interface EvaluationApiMock { evaluate: ReturnType<typeof vi.fn>; }

describe('ServiceJobOperationalAuthorizationComponent', () => {
  let fixture: ComponentFixture<ServiceJobOperationalAuthorizationComponent>;
  let api: EvaluationApiMock;
  let connectivity: { isOnline: ReturnType<typeof vi.fn> };
  const jobs = [{ id: 'job-1', jobNumber: 'JOB-001', summary: 'Brake work' }];
  const evaluation: OperationalAuthorizationEvaluation = {
    serviceJobId: 'job-1', serviceOrderId: 'order-1', status: 'FULLY_AUTHORIZED',
    totalLineCount: 2, authorizedLineCount: 2, pendingLineCount: 0, notAuthorizedLineCount: 0,
  };

  async function create(result = of(evaluation)): Promise<void> {
    api = { evaluate: vi.fn().mockReturnValue(result) };
    connectivity = { isOnline: vi.fn().mockReturnValue(true) };
    await TestBed.configureTestingModule({
      imports: [ServiceJobOperationalAuthorizationComponent],
      providers: [
        { provide: OperationalAuthorizationApiService, useValue: api },
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
});
