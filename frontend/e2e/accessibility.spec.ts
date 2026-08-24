import AxeBuilder from '@axe-core/playwright';
import { expect, test } from '@playwright/test';
import { installQuoteFixtures, installServiceProfitFixtures, openQuotes } from './fixtures';

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
  await page.getByRole('link', { name: 'Service Lines' }).click();
  await expect(page.getByRole('heading', { name: 'Service Lines' })).toBeVisible();
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

test('@a11y scans mobile Service Profit cards, filters, and routed detail', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await installServiceProfitFixtures(page);
  await page.goto('/service-profit');
  await expect(page.getByRole('link', { name: /Recover declined brake work/ })).toBeVisible();
  await expectAccessible(page);
  const filtersTrigger = page.getByRole('button', { name: /Sort & Filters/ });
  await filtersTrigger.click();
  await expect(page.getByRole('button', { name: 'Apply' })).toBeVisible();
  await expect(filtersTrigger).toHaveAttribute('aria-expanded', 'true');
  await expect(filtersTrigger).toHaveAttribute('aria-controls', 'service-profit-secondary-filters');
  await expectAccessible(page);
  await page.getByRole('link', { name: /Recover declined brake work/ }).click();
  await expect(page.getByRole('heading', { name: 'Previously declined work was identified' })).toBeVisible();
  await expectAccessible(page);
});