import { expect, test } from '@playwright/test';
import { installServiceProfitFixtures } from './fixtures';

const opportunityId = 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa';

test('opens the read-only opportunity workspace and returns to the exact queue state', async ({ page }) => {
  await installServiceProfitFixtures(page);
  await page.route('**/api/v1/service-profit/follow-ups*', (route) => route.fulfill({
    status: 200, contentType: 'application/json', body: JSON.stringify({
      items: [{ followUpId: 'follow-up-1', opportunityId, handlingStatus: 'OPEN', ownerPrincipalId: null, claimedAt: null, dueAt: '2026-08-26T10:00:00Z', disposition: 'FOLLOW_UP_REQUIRED', version: 0, createdAt: '2026-08-20T10:00:00Z', updatedAt: '2026-08-20T10:00:00Z', actionability: 'READY', opportunityStatus: 'DETECTED', evidenceClass: 'SOURCE_CONFIRMED', evidenceStrength: 'STRONG', priority: 'HIGH', title: 'Brake work recovery', summary: 'Previously declined brake replacement' }],
      page: 0, size: 25, totalElements: 1, totalPages: 1,
    }),
  }));
  await page.route(`**/api/v1/service-profit/opportunities/${opportunityId}`, (route) => route.fulfill({
    status: 200, contentType: 'application/json', body: JSON.stringify({
      id: opportunityId, opportunityType: 'DECLINED_WORK', status: 'DETECTED', evidenceClass: 'SOURCE_CONFIRMED', evidenceStrength: 'STRONG', priority: 'HIGH', actionability: 'READY', title: 'Brake work recovery', potentialAmount: 320, currencyCode: 'USD', detectedAt: '2026-08-20T10:00:00Z', sourceSystem: 'DMS', sourceEntityType: 'SERVICE_LINE', sourceEntityId: 'line-1', sourceServiceOrderId: null, sourceServiceJobId: null, sourceServiceLineId: 'line-1', sourceQuoteId: null, policyVersion: 'service-profit-r1', suppressionReason: null, suppressedAt: null, summary: 'Previously declined brake replacement', explanation: { headline: 'Declined work found', rationale: 'A source record confirms it.', evidenceBasis: 'Confirmed DMS service history.', recommendedAction: 'Contact the customer.' }, version: 1, createdAt: '2026-08-20T10:00:00Z', updatedAt: '2026-08-20T10:00:00Z', customerId: 'customer-internal', vehicleId: 'vehicle-internal', context: null,
    }),
  }));
  await page.route(`**/api/v1/service-profit/opportunities/${opportunityId}/follow-up`, (route) => route.fulfill({
    status: 200, contentType: 'application/json', body: JSON.stringify({ opportunityId, handlingStatus: 'OPEN', ownership: 'MINE', dueAt: '2026-08-29T10:00:00Z', dueState: 'UPCOMING', disposition: 'FOLLOW_UP_REQUIRED', version: 3 }),
  }));
  await page.route(`**/api/v1/service-profit/opportunities/${opportunityId}/follow-up/history`, (route) => route.fulfill({
    status: 200, contentType: 'application/json', body: JSON.stringify([
      { eventType: 'CREATED', actorType: 'SYSTEM', actorIdentity: 'AUTOVISION_SERVICE_PROFIT', applicationIdentity: 'AUTOVISION_SERVICE_PROFIT', occurredAt: '2026-08-27T10:10:00Z', previousValue: null, newValue: null },
      { eventType: 'OWNERSHIP_CLAIMED', actorType: 'HUMAN', actorIdentity: 'ME', applicationIdentity: null, occurredAt: '2026-08-27T10:15:00Z', previousValue: null, newValue: 'IN_PROGRESS' },
    ]),
  }));

  await page.goto('/service-profit/work-queue?ownership=MINE&dueState=OVERDUE&groupBy=PRIORITY&sort=OLDEST');
  await expect(page.getByRole('heading', { name: 'Service Profit Work Queue' }).first()).toBeVisible();
  await page.getByRole('link', { name: 'View details' }).click();
  await expect(page).toHaveURL(/service-profit\/opportunities\//);
  await expect(page.getByRole('heading', { name: 'Brake work recovery' })).toBeVisible();
  await expect(page.getByRole('tab', { name: 'Overview' })).toHaveAttribute('aria-selected', 'true');
  await page.getByRole('tab', { name: 'Evidence' }).click();
  await expect(page.getByText('Confirmed DMS service history.')).toBeVisible();
  await page.getByRole('tab', { name: 'Follow-up' }).click();
  await expect(page.getByText('Follow Up Required')).toBeVisible();
  await expect(page.getByText('Mine')).toBeVisible();
  await page.getByRole('tab', { name: 'History' }).click();
  await expect(page.getByText('Created')).toBeVisible();
  await expect(page.getByText('AutoVision Service Profit')).toBeVisible();
  await expect(page.getByText('Me', { exact: true })).toBeVisible();
  await page.goBack();
  await expect(page).toHaveURL(/service-profit\/work-queue\?ownership=MINE&dueState=OVERDUE&groupBy=PRIORITY&sort=OLDEST/);
  await page.setViewportSize({ width: 390, height: 844 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBe(true);
  await expect(page.getByRole('button', { name: /Claim|Assign|Complete|Reopen/ })).toHaveCount(0);
});