import { Routes } from '@angular/router';
import { authenticatedGuard } from './core/auth/auth.guard';

export const routes: Routes = [
	{
		path: 'service-profit/opportunities/:opportunityId',
		canActivate: [authenticatedGuard],
		loadComponent: () => import('./features/service-profit/service-profit-opportunity-detail-page.component').then((m) => m.ServiceProfitOpportunityDetailPageComponent),
	},
	{
		path: 'service-profit',
		canActivate: [authenticatedGuard],
		loadComponent: () => import('./features/service-profit/service-profit-manager.component').then((m) => m.ServiceProfitManagerComponent),
	},
	{
		path: 'service-profit/work-queue',
		canActivate: [authenticatedGuard],
		loadComponent: () => import('./features/service-profit/service-profit-work-queue.component').then((m) => m.ServiceProfitWorkQueueComponent),
	},
	{
		path: 'service-orders/:orderId',
		canActivate: [authenticatedGuard],
		loadComponent: () => import('./features/service-orders/service-order-workspace.component').then((m) => m.ServiceOrderWorkspaceComponent),
	},
	{
		path: '',
		loadComponent: () => import('./shell/home-page.component').then((m) => m.HomePageComponent),
	},
	{ path: '**', redirectTo: '' },
];
