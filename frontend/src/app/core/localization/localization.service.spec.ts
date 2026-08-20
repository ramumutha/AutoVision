import { TestBed } from '@angular/core/testing';
import { LocalizationService } from './localization.service';

describe('LocalizationService', () => {
  it('defaults to English shell resources', () => {
    const service = TestBed.inject(LocalizationService);
    expect(service.locale()).toBe('en');
    expect(service.text('appName')).toBe('AutoVision');
  });
});