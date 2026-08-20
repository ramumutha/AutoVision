import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter, convertToParamMap } from '@angular/router';
import { of, Subject } from 'rxjs';
import { ServiceOrderWorkspaceComponent } from './service-order-workspace.component';
import { ServiceOrderApiService } from './service-order-api.service';
import { ServiceOrderAggregate } from './service-order.models';

describe('ServiceOrderWorkspaceComponent', () => {
  let fixture: ComponentFixture<ServiceOrderWorkspaceComponent>;
  let api: { getServiceOrder: ReturnType<typeof vi.fn> };
  const aggregate: ServiceOrderAggregate = {
    order: {
      id: 'order-1', orderNumber: 'SO-100', vehicleId: 'vehicle-1', status: 'OPEN',
      openedAt: '2026-08-20T10:00:00Z', createdAt: '2026-08-20T10:00:00Z', updatedAt: '2026-08-20T10:00:00Z',
    },
    jobs: [], lines: [],
  };

  beforeEach(async () => {
    api = { getServiceOrder: vi.fn().mockReturnValue(of(aggregate)) };
    await TestBed.configureTestingModule({
      imports: [ServiceOrderWorkspaceComponent],
      providers: [
        provideRouter([]),
        { provide: ServiceOrderApiService, useValue: api },
        { provide: ActivatedRoute, useValue: {
          paramMap: of(convertToParamMap({ orderId: 'order-1' })),
          queryParamMap: of(convertToParamMap({ section: 'overview' })),
        } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ServiceOrderWorkspaceComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('loads and renders authoritative order context', () => {
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('SO-100');
    expect(text).toContain('OPEN');
    expect(text).toContain('Overview');
    expect(text).toContain('Service Lines');
    expect(text).toContain('Quotes');
    expect(text).not.toContain('tenant-');
  });

  it('uses a local retry path for a failed GET', async () => {
    const errors = new Subject<unknown>();
    api.getServiceOrder.mockReturnValueOnce(errors);
    fixture = TestBed.createComponent(ServiceOrderWorkspaceComponent);
    fixture.detectChanges();
    errors.error({ status: 404, message: 'Not found' });
    await fixture.whenStable();
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Service Order not found');
  });
});