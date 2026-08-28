import { Injectable, signal } from '@angular/core';

interface SiteConfig { productDemoUrl?: string; }

@Injectable({ providedIn: 'root' })
export class ProductDemoService {
  readonly url = signal<string | null>(null);

  async load(): Promise<void> {
    try {
      const response = await fetch('/assets/site-config.json', { cache: 'no-store' });
      if (response.ok) {
        const config = await response.json() as SiteConfig;
        const configuredUrl = config.productDemoUrl?.trim() || '';
        this.url.set(/^https:\/\//i.test(configuredUrl) ? configuredUrl : null);
      }
    } catch {
      this.url.set(null);
    }
  }
}