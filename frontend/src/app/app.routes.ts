import { Routes } from '@angular/router';
import { authenticatedGuard } from './core/auth/auth.guard';

export const routes: Routes = [
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
