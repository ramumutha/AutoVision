import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AvStatusComponent } from './av-status.component';

describe('AvStatusComponent', () => {
  let fixture: ComponentFixture<AvStatusComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [AvStatusComponent] }).compileComponents();
    fixture = TestBed.createComponent(AvStatusComponent);
    fixture.componentRef.setInput('label', 'Draft');
    fixture.componentRef.setInput('tone', 'info');
    await fixture.whenStable();
  });

  it('renders readable status text and status semantics', () => {
    const element = fixture.nativeElement.querySelector('[role="status"]') as HTMLElement;
    expect(element.textContent).toContain('Draft');
    expect(element.classList.contains('info')).toBe(true);
  });
});