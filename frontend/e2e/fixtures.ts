import { expect, Page } from '@playwright/test';

export const ORDER_ID = '11111111-1111-4111-8111-111111111111';
export const QUOTE_ID = '22222222-2222-4222-8222-222222222222';
export const CREATED_QUOTE_ID = '33333333-3333-4333-8333-333333333333';
export const SERVICE_PROFIT_OPPORTUNITY_ID = 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa';

const orderAggregate = {
  order: {
    id: ORDER_ID,
    orderNumber: 'SO-QUALITY-001',
    vehicleId: '44444444-4444-4444-8444-444444444444',
    status: 'OPEN',
    openedAt: '2026-08-20T08:00:00Z',
    completedAt: null,
    closedAt: null,
    cancelledAt: null,
    createdAt: '2026-08-20T08:00:00Z',
    updatedAt: '2026-08-20T08:00:00Z',
  },
  jobs: [{ id: '77777777-7777-4777-8777-777777777777', jobNumber: 'JOB-QUALITY-001', summary: 'Brake work' }],
  lines: [
    { id: '88888888-8888-4888-8888-888888888888', serviceOrderId: ORDER_ID, serviceJobId: null, lineNumber: 10, lineType: 'LABOR', description: 'Job-less diagnostic labor', quantity: 1.5, unitOfMeasure: 'HOUR', unitPrice: 100, currencyCode: 'EUR', netAmount: 150, taxAmount: 30, grossAmount: 180, hasCommercialSnapshot: true },
    { id: '99999999-9999-4999-8999-999999999999', serviceOrderId: ORDER_ID, serviceJobId: '77777777-7777-4777-8777-777777777777', lineNumber: 20, lineType: 'PART', description: 'Incomplete part snapshot', quantity: 2, unitOfMeasure: 'EA', unitPrice: null, currencyCode: null, netAmount: null, taxAmount: null, grossAmount: null, hasCommercialSnapshot: false },
  ],
};

export const existingQuote = {
  id: QUOTE_ID,
  serviceOrderId: ORDER_ID,
  quoteNumber: 'Q-QUALITY-001',
  status: 'ISSUED',
  currencyCode: 'EUR',
  validUntil: '2026-09-20T23:59:59+02:00',
  issuedAt: '2026-08-20T09:00:00Z',
  acceptedAt: null,
  declinedAt: null,
  cancelledAt: null,
  expiredAt: null,
  supersededAt: null,
  createdAt: '2026-08-20T08:30:00Z',
  updatedAt: '2026-08-20T09:00:00Z',
};

export const createdQuote = {
  ...existingQuote,
  id: CREATED_QUOTE_ID,
  quoteNumber: 'Q-QUALITY-002',
  status: 'DRAFT',
  validUntil: null,
};

export const quoteDetail = {
  ...existingQuote,
  termsSnapshot: 'Payment due on completion.',
  disclaimerSnapshot: 'Estimate subject to final inspection.',
  lines: [{
    id: '55555555-5555-4555-8555-555555555555',
    serviceQuoteId: QUOTE_ID,
    serviceLineId: '66666666-6666-4666-8666-666666666666',
    serviceJobId: null,
    descriptionSnapshot: 'Brake pad replacement',
    quantity: 2,
    unitPrice: 125,
    currencyCode: 'EUR',
    netAmount: 250,
    taxAmount: 50,
    grossAmount: 300,
    sequence: 0,
    createdAt: '2026-08-20T08:30:00Z',
  }],
};

export const createdQuoteDetail = {
  ...quoteDetail,
  ...createdQuote,
  lines: quoteDetail.lines.map((line) => ({ ...line, serviceQuoteId: CREATED_QUOTE_ID })),
};

export async function installQuoteFixtures(page: Page, options: { includeCreated?: boolean } = {}): Promise<void> {
  let quotes = options.includeCreated ? [existingQuote, createdQuote] : [existingQuote];
  await page.route('**/api/v1/service-orders/*/aggregate', (route) => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(orderAggregate) }));
  await page.route('**/api/v1/aftersales/service-orders/*/quotes', async (route) => {
    if (route.request().method() === 'POST') {
      const payload = route.request().postDataJSON() as Record<string, unknown>;
      expect(payload).toEqual(expect.objectContaining({ quoteNumber: 'Q-QUALITY-002', currencyCode: 'EUR', validUntil: null, termsSnapshot: null, disclaimerSnapshot: null }));
      expect(payload).not.toHaveProperty('serviceLineIds');
      expect(payload).not.toHaveProperty('selectedLines');
      quotes = [existingQuote, createdQuote];
      await route.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify(createdQuote) });
      return;
    }
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(quotes) });
  });
  await page.route('**/api/v1/aftersales/service-orders/*/quotes/*', (route) => {
    const quoteId = route.request().url().split('/').pop();
    const detail = quoteId === CREATED_QUOTE_ID ? createdQuoteDetail : quoteDetail;
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(detail) });
  });
}

export async function openQuotes(page: Page): Promise<void> {
  await page.goto(`/service-orders/${ORDER_ID}`);
  await expect(page.getByRole('heading', { name: 'SO-QUALITY-001' })).toBeVisible({ timeout: 15_000 });
  await page.getByRole('link', { name: 'Quotes' }).click();
  await expect(page.getByRole('heading', { name: 'Quotes' })).toBeVisible();
}

export async function installServiceProfitFixtures(page: Page): Promise<void> {
  const queueItem = {
    id: SERVICE_PROFIT_OPPORTUNITY_ID,
    tenantId: 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb',
    dealerId: 'cccccccc-cccc-4ccc-8ccc-cccccccccccc',
    branchId: 'dddddddd-dddd-4ddd-8ddd-dddddddddddd',
    locationId: 'eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee',
    opportunityKey: 'declined-brakes-1', opportunityType: 'DECLINED_WORK', status: 'SUPPRESSED',
    evidenceClass: 'SOURCE_CONFIRMED', evidenceStrength: 'STRONG', priority: 'HIGH', actionability: 'SUPPRESSED',
    title: 'Recover declined brake work', potentialAmount: 320, currencyCode: 'USD', detectedAt: '2026-08-20T10:00:00Z',
  };
  const summary = {
    totalOpportunities: 3, highPriorityCount: 1, reviewRequiredCount: 1, readyCount: 1, suppressedCount: 1,
    potentialByCurrency: [{ currencyCode: 'USD', amount: 320 }, { currencyCode: 'EUR', amount: 180 }],
    byOpportunityType: [], byPriority: [], byActionability: [],
  };
  const detail = {
    ...queueItem, customerId: 'ffffffff-ffff-4fff-8fff-ffffffffffff', vehicleId: '11111111-2222-4333-8444-555555555555',
    summary: 'Previously declined brake work.', sourceSystem: 'DMS', sourceEntityType: 'SERVICE_LINE', sourceEntityId: 'line-42',
    sourceServiceOrderId: ORDER_ID, sourceServiceJobId: null, sourceServiceLineId: null, sourceQuoteId: null,
    policyVersion: 'service-profit-r1', suppressionReason: 'WORK_ALREADY_COMPLETED', suppressedAt: '2026-08-21T10:00:00Z',
    explanation: { headline: 'Previously declined work was identified', rationale: 'The source service line records a customer decline.', evidenceBasis: 'Confirmed DMS service history.', recommendedAction: 'No action while suppression remains active.' },
    version: 1, createdAt: '2026-08-20T10:00:00Z', updatedAt: '2026-08-21T10:00:00Z',
  };
  const dataCapability = {
    assessmentState: 'ASSESSED', sourceDatasetId: 'AUTOVISION-SERVICE-PROFIT-R1-DEMO', sourceDatasetVersion: '1.0.0',
    assessmentPolicyVersion: 'service-profit-data-readiness-r1', assessedAt: '2026-08-24T05:00:00Z',
    capabilities: [
      { capability: 'REVENUE_ATTRIBUTION', status: 'AVAILABLE', reason: 'Invoice linkage supports revenue attribution.' },
      { capability: 'GROSS_PROFIT_ATTRIBUTION', status: 'PARTIAL', reason: 'Cost data is incomplete.' },
    ],
  };

  await page.route('**/api/v1/service-profit/data-capability', (route) => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(dataCapability) }));
  await page.route('**/api/v1/service-profit/opportunities/summary*', (route) => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(summary) }));
  await page.route(`**/api/v1/service-profit/opportunities/${SERVICE_PROFIT_OPPORTUNITY_ID}`, (route) => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(detail) }));
  await page.route('**/api/v1/service-profit/opportunities?*', (route) => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ items: [queueItem], page: 0, size: 25, totalElements: 1, totalPages: 1 }) }));
}