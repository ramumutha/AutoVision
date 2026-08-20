import { Injectable, signal } from '@angular/core';

export interface BrandingConfig {
  productName: string;
  organizationName: string;
  logoUrl?: string;
  faviconUrl?: string;
  mandatoryOrganizationIdentity: boolean;
}

export interface OrganizationBranding {
  organizationName: string;
  logoUrl?: string;
  faviconUrl?: string;
}

@Injectable({ providedIn: 'root' })
export class BrandingService {
  private readonly platform: BrandingConfig = {
    productName: 'AutoVision',
    organizationName: 'AutoVision',
    mandatoryOrganizationIdentity: false,
  };
  readonly branding = signal<BrandingConfig>(this.platform);

  applyOrganizationBranding(organization: OrganizationBranding): void {
    this.branding.set({
      ...this.platform,
      ...organization,
      mandatoryOrganizationIdentity: true,
    });
    if (organization.faviconUrl) {
      this.applyFavicon(organization.faviconUrl);
    }
    document.title = this.branding().organizationName;
  }

  private applyFavicon(url: string): void {
    const link = document.querySelector<HTMLLinkElement>('link[rel="icon"]') ?? document.createElement('link');
    link.rel = 'icon';
    link.href = url;
    document.head.appendChild(link);
  }
}