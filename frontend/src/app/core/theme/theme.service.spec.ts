import { TestBed } from '@angular/core/testing';
import { BrandingService } from '../branding/branding.service';
import { ThemeService } from './theme.service';

describe('ThemeService and BrandingService', () => {
  it('keeps mandatory organization identity while allowing user appearance preference', () => {
    const branding = TestBed.inject(BrandingService);
    const theme = TestBed.inject(ThemeService);
    branding.applyOrganizationBranding({ organizationName: 'Dealer North', logoUrl: '/dealer.svg' });
    theme.resolve({ brandPrimary: '#0a6b5f' }, { appearance: 'DARK' });

    expect(branding.branding().organizationName).toBe('Dealer North');
    expect(branding.branding().logoUrl).toBe('/dealer.svg');
    expect(theme.theme().appearance).toBe('DARK');
    expect(theme.theme().brandPrimary).toBe('#0a6b5f');
  });

  it('falls back when an unsafe theme value is supplied', () => {
    const theme = TestBed.inject(ThemeService);
    theme.resolve({ brandPrimary: 'url(javascript:alert(1))' });
    expect(theme.theme().brandPrimary).toBe('#006b5f');
  });
});