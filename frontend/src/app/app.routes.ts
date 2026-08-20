import { Routes } from '@angular/router';

export const routes: Routes = [
	{
		path: '',
		loadComponent: () => import('./shell/home-page.component').then((m) => m.HomePageComponent),
	},
	{ path: '**', redirectTo: '' },
];
