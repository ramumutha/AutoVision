import { Injectable, signal } from '@angular/core';

export interface ProductDemoEntry { label: string; url: string | null; }
interface SiteConfig { productDemo?: { serviceProfit?: { label?: string; url?: string } }; productDemoUrl?: string; }

@Injectable({ providedIn: 'root' })
export class ProductDemoService {
  readonly url = signal<string | null>(null);
  readonly entries = signal<ProductDemoEntry[]>([]);

  async load(): Promise<void> {
    try {
      const response = await fetch('/assets/site-config.json', { cache: 'no-store' });
      if (response.ok) {
        const config = await response.json() as SiteConfig;
        const configuredUrl = config.productDemo?.serviceProfit?.url?.trim() || config.productDemoUrl?.trim() || '';
        const parsedUrl = configuredUrl ? new URL(configuredUrl) : null;
        const isLocalDevelopmentUrl = parsedUrl?.protocol === 'http:' && ['localhost', '127.0.0.1', '[::1]'].includes(parsedUrl.hostname);
        const url = parsedUrl && (parsedUrl.protocol === 'https:' || isLocalDevelopmentUrl) ? parsedUrl.toString() : null;
        this.url.set(url);
        this.entries.set([{ label: config.productDemo?.serviceProfit?.label?.trim() || 'Service Profit AI', url }]);
      }
    } catch {
      this.url.set(null);
    }
  }
}