import { expect, test } from '@playwright/test';
import { CREATED_QUOTE_ID, ORDER_ID, installQuoteFixtures, openQuotes } from './fixtures';

test('completes the ServiceOrder -> Quotes -> Create -> Detail journey', async ({ page }) => {
  await installQuoteFixtures(page);
  await openQuotes(page);
  await expect(page.getByRole('button', { name: 'Q-QUALITY-001' })).toBeVisible();

  await page.getByRole('button', { name: 'Create Quote' }).click();
  await expect(page.getByRole('heading', { name: 'Create Quote' })).toBeVisible();
  await expect(page.getByLabel('Quote Number')).toBeVisible();
  await expect(page.getByLabel('Currency')).toBeVisible();
  await page.getByLabel('Quote Number').fill('Q-QUALITY-002');
  await page.getByLabel('Currency').fill('EUR');
  await page.getByRole('button', { name: 'Create', exact: true }).click();

  await expect(page.getByText('Quote created successfully')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Q-QUALITY-002' })).toBeVisible();
  await page.getByRole('button', { name: 'Q-QUALITY-002' }).click();
  await expect(page).toHaveURL(new RegExp(`orderId=${ORDER_ID}|quoteId=${CREATED_QUOTE_ID}`));
  await expect(page.getByRole('heading', { name: 'Q-QUALITY-002' })).toBeVisible();
  await expect(page.locator('app-service-quote-detail').getByText('DRAFT', { exact: true }).first()).toBeVisible();
  await expect(page.getByRole('rowheader', { name: 'Brake pad replacement' })).toBeVisible();
});

test('supports the focused keyboard path to Create Quote', async ({ page }) => {
  await installQuoteFixtures(page);
  await openQuotes(page);
  await page.getByRole('link', { name: 'Quotes' }).focus();
  await page.keyboard.press('Enter');
  await page.getByRole('button', { name: 'Create Quote' }).focus();
  await page.keyboard.press('Enter');
  await expect(page.getByRole('heading', { name: 'Create Quote' })).toBeVisible();
  await page.getByLabel('Quote Number').focus();
  await page.keyboard.type('Q-KEYBOARD');
  await page.getByLabel('Currency').focus();
  await page.keyboard.type('EUR');
  await expect(page.getByRole('button', { name: 'Create', exact: true })).toBeEnabled();
});