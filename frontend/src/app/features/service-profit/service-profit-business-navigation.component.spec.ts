import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
  ServiceProfitBusinessLens,
  ServiceProfitBusinessNavigationComponent,
} from './service-profit-business-navigation.component';
import { ServiceProfitOpportunitySummary } from './service-profit.models';

describe('ServiceProfitBusinessNavigationComponent', () => {
  let fixture: ComponentFixture<ServiceProfitBusinessNavigationComponent>;
  let selected: ServiceProfitBusinessLens[];

  const summary: ServiceProfitOpportunitySummary = {
    totalOpportunities: 10,
    highPriorityCount: 5,
    reviewRequiredCount: 1,
    readyCount: 8,
    suppressedCount: 1,
    potentialByCurrency: [],
    byOpportunityType: [],
    byPriority: [],
    byActionability: [],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ServiceProfitBusinessNavigationComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ServiceProfitBusinessNavigationComponent);
    fixture.componentRef.setInput('summary', summary);
    fixture.componentRef.setInput('selectedLens', 'TOTAL');
    selected = [];
    fixture.componentInstance.lensSelected.subscribe((lens) => selected.push(lens));
    fixture.detectChanges();
  });

  it('renders four native business-navigation buttons with authoritative counts', () => {
    const element = fixture.nativeElement as HTMLElement;
    const group = element.querySelector<HTMLElement>('[role="group"]');
    const buttons = Array.from(element.querySelectorAll<HTMLButtonElement>('button'));

    expect(group?.getAttribute('aria-label')).toBe('Opportunity business views');
    expect(buttons).toHaveLength(4);
    expect(buttons.every((button) => button.type === 'button')).toBe(true);
    expect(buttons.map((button) => button.querySelector('.business-label')?.textContent?.trim())).toEqual([
      'Total Opportunities',
      'High Priority',
      'Review Required',
      'Ready to Action',
    ]);
    expect(buttons.map((button) => button.querySelector('strong')?.textContent?.trim())).toEqual(['10', '5', '1', '8']);
  });

  it('exposes the selected state with aria-pressed and visible text', () => {
    const buttons = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('button'));

    expect(buttons.map((button) => button.getAttribute('aria-pressed'))).toEqual([
      'true',
      'false',
      'false',
      'false',
    ]);
    expect(buttons[0].querySelector('.selected-state')?.textContent).toContain('Selected');
  });

  it('emits the selected business lens through a keyboard-focusable native control', () => {
    const ready = (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('button')[3];

    ready.focus();
    expect(document.activeElement).toBe(ready);
    ready.click();

    expect(selected).toEqual(['READY_TO_ACTION']);
  });

  it('never presents raw internal lens values', () => {
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).not.toContain('HIGH_PRIORITY');
    expect(text).not.toContain('REVIEW_REQUIRED');
    expect(text).not.toContain('READY_TO_ACTION');
  });
});