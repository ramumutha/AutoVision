import { expect, test } from '@playwright/test';
import { installQuoteFixtures, openQuotes } from './fixtures';

test('loads ServiceOrder Quotes and opens detail', async ({ page }) => {
  await installQuoteFixtures(page);
  await openQuotes(page);
  await expect(page.getByRole('button', { name: 'Q-QUALITY-001' })).toBeVisible();
  await page.getByRole('button', { name: 'Q-QUALITY-001' }).click();
  await expect(page.getByRole('heading', { name: 'Q-QUALITY-001' })).toBeVisible();
  await expect(page.getByRole('rowheader', { name: 'Brake pad replacement' })).toBeVisible();
});