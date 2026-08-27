import { expect, test } from '@playwright/test';
import { installServiceProfitFixtures } from './fixtures';

const followUp = (overrides: Record<string, unknown> = {}) => ({
  followUpId: 'follow-up-1', opportunityId: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', handlingStatus: 'OPEN', ownerPrincipalId: null,
  claimedAt: null, dueAt: '2026-08-26T10:00:00Z', disposition: 'FOLLOW_UP_REQUIRED', version: 0,
  createdAt: '2026-08-20T10:00:00Z', updatedAt: '2026-08-20T10:00:00Z', actionability: 'READY', opportunityStatus: 'DETECTED',
  evidenceClass: 'SOURCE_CONFIRMED', evidenceStrength: 'STRONG', priority: 'HIGH', title: 'Brake work recovery',
  summary: 'Previously declined brake replacement', ...overrides,
});

async function installQueueFixtures(page: Parameters<typeof installServiceProfitFixtures>[0]): Promise<void> {
  await installServiceProfitFixtures(page);
  await page.route('**/api/v1/service-profit/follow-ups*', (route) => route.fulfill({
    status: 200, contentType: 'application/json', body: JSON.stringify({
      items: [followUp(), followUp({ followUpId: 'follow-up-2', opportunityId: 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', dueAt: null, disposition: 'NO_RESPONSE_RECORDED', priority: 'MEDIUM', title: 'Inspection follow-up', summary: 'No response after the first follow-up' })],
      page: 0, size: 25, totalElements: 2, totalPages: 1,
    }),
  }));
}

test.describe('Service Profit work queue', () => {
  test('opens as grouped semantic cards with safe read-only fields', async ({ page }) => {
    await installQueueFixtures(page);
    await page.goto('/service-profit/work-queue');
    await expect(page.getByRole('heading', { name: 'Service Profit Work Queue' }).first()).toBeVisible();
    await expect(page.locator('app-service-profit-opportunity-card')).toHaveCount(2);
    await expect(page.locator('table')).toHaveCount(0);
    await expect(page.getByText('Brake work recovery')).toBeVisible();
    await expect(page.getByText('Previously declined brake replacement')).toBeVisible();
    await expect(page.getByText('tenant-1')).toHaveCount(0);
    await expect(page.getByText('Revenue')).toHaveCount(0);
    await expect(page.getByRole('button', { name: /Claim|Assign|Complete|Reopen/ })).toHaveCount(0);
  });

  test('persists ownership, filters, grouping and sort in the URL', async ({ page }) => {
    await installQueueFixtures(page);
    await page.goto('/service-profit/work-queue');
    await page.getByRole('button', { name: 'Mine' }).click();
    await expect(page).toHaveURL(/ownership=MINE/);
    await page.getByRole('button', { name: 'Filters' }).click();
    await page.locator('#queue-filters select').nth(1).selectOption('OVERDUE');
    await page.getByRole('button', { name: 'Apply' }).click();
    await expect(page).toHaveURL(/dueState=OVERDUE/);
    await page.locator('.controls select').nth(0).selectOption('DISPOSITION');
    await expect(page).toHaveURL(/groupBy=DISPOSITION/);
    await page.locator('.controls select').nth(1).selectOption('NEWEST');
    await expect(page).toHaveURL(/sort=NEWEST/);
    await page.reload();
    await expect(page.locator('.controls select').nth(0)).toHaveValue('DISPOSITION');
    await expect(page.locator('.controls select').nth(1)).toHaveValue('NEWEST');
    await expect(page.getByRole('button', { name: 'Mine' })).toHaveAttribute('aria-pressed', 'true');
  });

  test('Clear restores default filter state and mobile remains usable', async ({ page }) => {
    await installQueueFixtures(page);
    await page.goto('/service-profit/work-queue?ownership=MINE&handlingStatus=COMPLETED&dueState=OVERDUE&groupBy=PRIORITY&sort=OLDEST');
    await page.getByRole('button', { name: 'Filters' }).click();
    await page.getByRole('button', { name: 'Clear filters' }).click();
    await expect(page).not.toHaveURL(/handlingStatus|dueState/);
    await expect(page).toHaveURL(/service-profit\/work-queue/);
    await page.setViewportSize({ width: 390, height: 844 });
    await expect(page.locator('app-service-profit-opportunity-card').first()).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBe(true);
  });
});
