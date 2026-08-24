import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ServiceProfitApiService } from './service-profit-api.service';
import { ServiceProfitGroupedQueueService } from './service-profit-grouped-queue.service';
import { ServiceProfitOpportunityPage, ServiceProfitOpportunityQueueItem, ServiceProfitOpportunitySummary } from './service-profit.models';

describe('ServiceProfitGroupedQueueService', () => {
  const item = (id: string): ServiceProfitOpportunityQueueItem => ({
    id, tenantId: 'tenant-1', dealerId: 'dealer-1', branchId: 'branch-1', locationId: 'location-1',
    opportunityKey: id, opportunityType: 'DECLINED_WORK', status: 'DETECTED', evidenceClass: 'SOURCE_CONFIRMED',
    evidenceStrength: 'STRONG', priority: 'HIGH', actionability: 'READY', title: id,
    potentialAmount: 100, currencyCode: 'USD', detectedAt: '2026-08-24T00:00:00Z',
  });
  const summary: ServiceProfitOpportunitySummary = {
    totalOpportunities: 3, highPriorityCount: 2, reviewRequiredCount: 1, readyCount: 2, suppressedCount: 0,
    potentialByCurrency: [],
    byOpportunityType: [{ key: 'DECLINED_WORK', count: 2 }, { key: 'DEFERRED_WORK', count: 1 }, { key: 'DUE_SERVICE', count: 0 }],
    byPriority: [{ key: 'HIGH', count: 2 }, { key: 'MEDIUM', count: 1 }],
    byActionability: [{ key: 'READY', count: 2 }, { key: 'REVIEW_REQUIRED', count: 1 }],
  };
  let api: { getOpportunities: ReturnType<typeof vi.fn> };
  let service: ServiceProfitGroupedQueueService;

  beforeEach(() => {
    api = { getOpportunities: vi.fn() };
    TestBed.configureTestingModule({ providers: [ServiceProfitGroupedQueueService, { provide: ServiceProfitApiService, useValue: api }] });
    service = TestBed.inject(ServiceProfitGroupedQueueService);
  });

  it.each([
    ['OPPORTUNITY_TYPE', ['DECLINED_WORK', 'DEFERRED_WORK']],
    ['PRIORITY', ['HIGH', 'MEDIUM']],
    ['ACTIONABILITY', ['READY', 'REVIEW_REQUIRED']],
  ] as const)('loads non-empty %s groups using authoritative facet counts', async (groupBy, keys) => {
    api.getOpportunities.mockImplementation((query) => of(page([item(query.opportunityType ?? query.priority ?? query.actionability)])));

    const groups = await service.load(summary, groupBy, {}, 'DETECTED_DESC').toPromise();

    expect(groups?.map((group) => group.key)).toEqual(keys);
    expect(groups?.map((group) => group.authoritativeCount)).toEqual([2, 1]);
    expect(api.getOpportunities).toHaveBeenCalledTimes(2);
    expect(api.getOpportunities).toHaveBeenCalledWith(expect.objectContaining({ page: 0, size: 100, sort: 'DETECTED_DESC' }));
  });

  it('returns one complete flat queue for None', async () => {
    api.getOpportunities.mockReturnValue(of(page([item('one'), item('two'), item('three')])));

    const groups = await service.load(summary, 'NONE', {}, 'POTENTIAL_DESC').toPromise();

    expect(groups).toEqual([{ key: 'ALL', authoritativeCount: 3, opportunities: [item('one'), item('two'), item('three')], complete: true }]);
  });

  it('composes group queries with existing filters', async () => {
    api.getOpportunities.mockReturnValue(of(page([item('group-item')])));

    await service.load(summary, 'OPPORTUNITY_TYPE', { priority: 'HIGH', actionability: 'READY' }, 'DETECTED_ASC').toPromise();

    expect(api.getOpportunities).toHaveBeenCalledWith(expect.objectContaining({
      opportunityType: 'DECLINED_WORK', priority: 'HIGH', actionability: 'READY', sort: 'DETECTED_ASC',
    }));
  });

  it('fetches every server page before presenting a group', async () => {
    api.getOpportunities.mockImplementation((query) => of(query.page === 0
      ? page([item('first')], 0, 2, 101)
      : page([item('last')], 1, 2, 101)));

    const groups = await service.load({ ...summary, totalOpportunities: 101 }, 'NONE', {}, 'DETECTED_DESC').toPromise();

    expect(api.getOpportunities).toHaveBeenCalledTimes(2);
    expect(groups?.[0].authoritativeCount).toBe(101);
    expect(groups?.[0].complete).toBe(false);
    expect(groups?.[0].opportunities.map((opportunity) => opportunity.id)).toEqual(['first', 'last']);
  });

  function page(
    items: ServiceProfitOpportunityQueueItem[],
    pageNumber = 0,
    totalPages = 1,
    totalElements = items.length,
  ): ServiceProfitOpportunityPage {
    return { items, page: pageNumber, size: 100, totalElements, totalPages };
  }
});