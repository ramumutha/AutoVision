import AxeBuilder from '@axe-core/playwright';
import { expect, test } from '@playwright/test';
import { installQuoteFixtures, openQuotes } from './fixtures';

async function expectAccessible(page: Parameters<typeof installQuoteFixtures>[0]): Promise<void> {
  const result = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze();
  const serious = result.violations.filter((violation) => violation.impact === 'critical' || violation.impact === 'serious');
  expect(serious, serious.map((violation) => `${violation.id}: ${violation.help}`).join('\n')).toEqual([]);
}

test('@a11y scans Overview, Quotes, Create Quote, and Quote Detail', async ({ page }) => {
  await installQuoteFixtures(page);
  await page.goto('/service-orders/11111111-1111-4111-8111-111111111111');
  await expect(page.getByRole('heading', { name: 'SO-QUALITY-001' })).toBeVisible();
  await expectAccessible(page);
  await openQuotes(page);
  await expectAccessible(page);
  await page.getByRole('button', { name: 'Create Quote' }).click();
  await expectAccessible(page);
  await page.getByLabel('Cancel').click();
  await page.getByRole('button', { name: 'Q-QUALITY-001' }).click();
  await expect(page.getByRole('rowheader', { name: 'Brake pad replacement' })).toBeVisible();
  await expectAccessible(page);
});