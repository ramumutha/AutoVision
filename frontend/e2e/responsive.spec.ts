import { expect, test } from '@playwright/test';
import { installQuoteFixtures, openQuotes } from './fixtures';

const viewports = [
  ['large desktop', { width: 1920, height: 1080 }],
  ['desktop', { width: 1440, height: 900 }],
  ['tablet landscape', { width: 1024, height: 768 }],
  ['tablet portrait', { width: 768, height: 1024 }],
  ['mobile', { width: 390, height: 844 }],
] as const;

for (const [name, viewport] of viewports) {
  test(`@responsive ${name} keeps the Quote workflow usable`, async ({ page }) => {
    await page.setViewportSize(viewport);
    await installQuoteFixtures(page);
    await openQuotes(page);
    await page.getByRole('button', { name: 'Create Quote' }).click();
    await expect(page.getByLabel('Quote Number')).toBeVisible();
    await page.getByLabel('Cancel').click();
    await page.getByRole('button', { name: 'Q-QUALITY-001' }).click();
    const detailLine = name === 'mobile'
      ? page.locator('.line-cards').getByText('Brake pad replacement', { exact: true })
      : page.getByRole('rowheader', { name: 'Brake pad replacement' });
    await expect(detailLine).toBeVisible();
    if (name === 'mobile') await expect(page.getByRole('button', { name: 'Back to Quotes' })).toBeVisible();
  });
}