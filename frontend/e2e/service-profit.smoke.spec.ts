import { expect, test } from '@playwright/test';
import { installServiceProfitFixtures } from './fixtures';

test('loads the Service Profit manager queue and authoritative explanation responsively', async ({ page }) => {
  await installServiceProfitFixtures(page);
  await page.goto('/service-profit');

  await expect(page.getByRole('heading', { name: 'Service Profit Manager' })).toBeVisible();
  const currencyValues = page.locator('.currency-values p');
  await expect(currencyValues).toHaveCount(2);
  await expect(currencyValues.nth(0)).toHaveText('$320');
  await expect(currencyValues.nth(1)).toHaveText('€180');

  await page.getByRole('button', { name: 'Recover declined brake work' }).click();
  await expect(page.getByRole('heading', { name: 'Previously declined work was identified' })).toBeVisible();
  await expect(page.getByText('Confirmed DMS service history.')).toBeVisible();
  await expect(page.getByText('Suppressed opportunity')).toBeVisible();

  await page.setViewportSize({ width: 390, height: 844 });
  await expect(page.getByRole('heading', { name: 'Service Profit Manager' })).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBe(true);
});