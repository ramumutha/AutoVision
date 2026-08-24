import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'avMoney',
  standalone: true,
})
export class AvMoneyPipe implements PipeTransform {
  transform(
    amount: number | null | undefined,
    currencyCode: string | null | undefined,
    locale = 'en',
  ): string {
    if (amount === null || amount === undefined) return '';

    const currency = currencyCode?.trim().toUpperCase();
    if (!currency) return this.formatNumber(amount, locale);

    try {
      return new Intl.NumberFormat(locale || 'en', {
        style: 'currency',
        currency,
        currencyDisplay: 'symbol',
        minimumFractionDigits: 0,
        maximumFractionDigits: 2,
      }).format(amount);
    } catch {
      return `${this.formatNumber(amount, locale)} ${currency}`;
    }
  }

  private formatNumber(amount: number, locale: string): string {
    try {
      return new Intl.NumberFormat(locale || 'en', {
        minimumFractionDigits: 0,
        maximumFractionDigits: 2,
      }).format(amount);
    } catch {
      return new Intl.NumberFormat('en', {
        minimumFractionDigits: 0,
        maximumFractionDigits: 2,
      }).format(amount);
    }
  }
}