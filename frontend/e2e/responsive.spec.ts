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
    const businessNavigation = page.getByRole('group', { name: 'Opportunity business views' });
    await expect(businessNavigation).toBeVisible();
    await expect(page.locator('.opportunity-type-navigation')).toHaveCount(0);
    await expect(page.locator('.filter-panel')).toHaveCount(0);
    const recoverablePotential = page.locator('.kpi-primary');
    await expect(recoverablePotential.getByText('$320')).toBeVisible();
    await recoverablePotential.getByRole('button', { name: 'View details' }).click();
    await expect(page.getByRole('dialog', { name: 'Data capability' })).toBeVisible();
    await expect(page.getByRole('dialog').getByText('Revenue data')).toBeVisible();
    await page.getByRole('button', { name: 'Hide details' }).click();
    const groupHeading = page.getByRole('button', { name: /Declined Work.*1/ });
    await expect(groupHeading).toBeVisible();
    await expect(groupHeading).toHaveAttribute('aria-expanded', 'false');
    await groupHeading.click();
    await expect(groupHeading).toHaveAttribute('aria-expanded', 'true');

    if (mobile) {
      const kpiButtons = businessNavigation.getByRole('button');
      await expect(kpiButtons).toHaveCount(4);
      const firstBox = await kpiButtons.nth(0).boundingBox();
      const secondBox = await kpiButtons.nth(1).boundingBox();
      const thirdBox = await kpiButtons.nth(2).boundingBox();
      expect(firstBox).not.toBeNull();
      expect(secondBox?.y).toBe(firstBox?.y);
      expect(thirdBox?.y).toBeGreaterThan(firstBox?.y ?? 0);
      const opportunityCard = page.getByRole('link', { name: /Recover declined brake work/ });
      await expect(opportunityCard).toBeVisible();
      await expect(opportunityCard).not.toContainText('Declined Work');
      await expect(opportunityCard).not.toContainText('Strong evidence');
      await expect(page.getByLabel('Group by')).toHaveValue('OPPORTUNITY_TYPE');
      await page.getByRole('link', { name: /Recover declined brake work/ }).click();
      await expect(page).toHaveURL(/service-profit\/opportunities\/.*priority=HIGH.*sort=POTENTIAL_DESC/);
      await expect(page.getByRole('heading', { name: 'Previously declined work was identified' })).toBeVisible();
      await page.getByRole('button', { name: 'Back to opportunities' }).click();
      await expect(page).toHaveURL(/service-profit\?priority=HIGH&sort=POTENTIAL_DESC/);
    } else {
      await expect(page.getByRole('button', { name: 'Refresh Service Profit data' })).toBeVisible();
      await expect(page.getByRole('button', { name: /Filters|Clear filters|Sort & Filters/ })).toHaveCount(0);
      await expect(page.locator('.desktop-sort')).toHaveCount(0);
      await expect(page.getByLabel('Group by')).toHaveValue('OPPORTUNITY_TYPE');
      await expect(page.getByRole('table')).toBeVisible();
      await expect(page.getByRole('columnheader', { name: /Potential/ })).toHaveAttribute('aria-sort', 'descending');
      await expect(page.locator('th.secondary-tablet').filter({ hasText: 'Detected' })).toHaveAttribute('aria-sort', 'none');
      await page.getByRole('button', { name: 'Recover declined brake work' }).click();
      await expect(page.getByRole('heading', { name: 'Previously declined work was identified' })).toBeVisible();
    }

    expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBe(true);
  });
}

test.skip('@responsive Service Profit secondary filters preserve URL and browser history', async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 900 });
  await installServiceProfitFixtures(page);
  await page.goto('/service-profit?type=DECLINED_WORK&sort=POTENTIAL_DESC');

  const trigger = page.getByRole('button', { name: /^Filters/ });
  await trigger.click();
  const panel = page.locator('.filter-panel');
  await expect(panel.getByLabel('Opportunity Type')).toHaveValue('DECLINED_WORK');
  await expect(panel.getByLabel('Sort by')).toHaveValue('POTENTIAL_DESC');
  await panel.getByLabel('Opportunity Type').selectOption('DEFERRED_WORK');
  await panel.getByLabel('Priority').selectOption('HIGH');
  await expect(page).toHaveURL(/type=DECLINED_WORK.*sort=POTENTIAL_DESC/);

  await panel.getByRole('button', { name: 'Apply' }).click();
  await expect(trigger).toBeFocused();
  await expect.poll(() => Object.fromEntries(new URL(page.url()).searchParams)).toEqual({
    type: 'DEFERRED_WORK', sort: 'POTENTIAL_DESC', priority: 'HIGH',
  });
  await expect(page.getByRole('group', { name: 'Opportunity business views' }).getByRole('button', { name: /High Priority/ })).toHaveAttribute('aria-pressed', 'true');
  await expect(trigger.locator('.active-count')).toHaveText('1');

  await page.goBack();
  await expect(page).toHaveURL(/type=DECLINED_WORK.*sort=POTENTIAL_DESC/);
  await page.goForward();
  await expect.poll(() => Object.fromEntries(new URL(page.url()).searchParams)).toEqual({
    type: 'DEFERRED_WORK', sort: 'POTENTIAL_DESC', priority: 'HIGH',
  });

  await trigger.click();
  await panel.getByRole('button', { name: 'Clear filters' }).click();
  await panel.getByRole('button', { name: 'Apply' }).click();
  await expect(page).toHaveURL(/sort=POTENTIAL_DESC/);
  expect(new URL(page.url()).searchParams.has('type')).toBe(false);
  expect(new URL(page.url()).searchParams.has('priority')).toBe(false);
  expect(new URL(page.url()).searchParams.has('actionability')).toBe(false);
});