import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ServiceOrderLinesComponent } from './service-order-lines.component';

describe('ServiceOrderLinesComponent', () => {
  let fixture: ComponentFixture<ServiceOrderLinesComponent>;
  const lines = [
    {
      id: 'line-1', serviceOrderId: 'order-1', serviceJobId: null, lineNumber: 10,
      lineType: 'LABOR', description: 'Job-less diagnostic labor', quantity: 1.5,
      unitOfMeasure: 'HOUR', unitPrice: 100, currencyCode: 'EUR', netAmount: 150,
      taxAmount: 30, grossAmount: 180, hasCommercialSnapshot: true,
    },
    {
      id: 'line-2', serviceOrderId: 'order-1', serviceJobId: 'job-1', lineNumber: 20,
      lineType: 'PART', description: 'Incomplete part snapshot', quantity: 2,
      unitOfMeasure: 'EA', unitPrice: null, currencyCode: null, netAmount: null,
      taxAmount: null, grossAmount: null, hasCommercialSnapshot: false,
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ServiceOrderLinesComponent] }).compileComponents();
    fixture = TestBed.createComponent(ServiceOrderLinesComponent);
    fixture.componentRef.setInput('lines', lines);
    fixture.componentRef.setInput('jobs', [{ id: 'job-1', jobNumber: 'JOB-001', summary: 'Brake work' }]);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('renders ready, incomplete, and job-less lines without quote eligibility logic', () => {
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Job-less diagnostic labor');
    expect(text).toContain('JOB-001');
    expect(text).toContain('Commercial data available');
    expect(text).toContain('Commercial data incomplete');
    expect(text).toContain('€');
    expect(text).toContain('180');
    expect(text).toContain('Eligible service lines are selected automatically when a quote is created.');
    expect(text).not.toContain('Eligible for Quote');
    expect(text).not.toContain('line-1');
    expect(text).not.toContain('job-1');
    expect(text).not.toContain('Select All for Quote');
  });

  it('renders the localized empty state', async () => {
    fixture.componentRef.setInput('lines', []);
    fixture.detectChanges();
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('No service lines have been added to this service order.');
  });
});