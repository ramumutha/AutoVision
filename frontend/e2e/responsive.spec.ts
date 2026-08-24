import { expect, test } from '@playwright/test';
import { installQuoteFixtures, installServiceProfitFixtures, openQuotes } from './fixtures';

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

test('@responsive Service Lines remain readable on mobile', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await installQuoteFixtures(page);
  await page.goto('/service-orders/11111111-1111-4111-8111-111111111111?section=lines');
  await expect(page.getByRole('heading', { name: 'Service Lines' })).toBeVisible();
  await expect(page.locator('.cards').getByText(/Job-less diagnostic labor/)).toBeVisible();
  await expect(page.locator('.cards').getByText('Commercial data incomplete')).toBeVisible();
  await expect(page.getByText('Eligible service lines are selected automatically when a quote is created.')).toBeVisible();
});

const serviceProfitViewports = [
  ['mobile 390', { width: 390, height: 844 }, true],
  ['mobile 430', { width: 430, height: 932 }, true],
  ['tablet portrait', { width: 768, height: 1024 }, false],
  ['tablet landscape', { width: 1024, height: 768 }, false],
  ['desktop', { width: 1440, height: 900 }, false],
  ['large desktop', { width: 1920, height: 1080 }, false],
] as const;

for (const [name, viewport, mobile] of serviceProfitViewports) {
  test(`@responsive ${name} uses the intended Service Profit interaction`, async ({ page }) => {
    await page.setViewportSize(viewport);
    await installServiceProfitFixtures(page);
    await page.goto('/service-profit?priority=HIGH&sort=POTENTIAL_DESC');
    await expect(page.getByRole('heading', { name: 'Service Profit Manager' })).toBeVisible();

    if (mobile) {
      await expect(page.getByRole('link', { name: /Recover declined brake work/ })).toBeVisible();
      await expect(page.getByRole('button', { name: /Sort & Filter/ })).toBeVisible();
      await page.getByRole('link', { name: /Recover declined brake work/ }).click();
      await expect(page).toHaveURL(/service-profit\/opportunities\/.*priority=HIGH.*sort=POTENTIAL_DESC/);
      await expect(page.getByRole('heading', { name: 'Previously declined work was identified' })).toBeVisible();
      await page.getByRole('button', { name: 'Back to opportunities' }).click();
      await expect(page).toHaveURL(/service-profit\?priority=HIGH&sort=POTENTIAL_DESC/);
    } else {
      await expect(page.getByRole('table')).toBeVisible();
      await page.getByRole('button', { name: 'Recover declined brake work' }).click();
      await expect(page.getByRole('heading', { name: 'Previously declined work was identified' })).toBeVisible();
    }

    expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBe(true);
  });
}