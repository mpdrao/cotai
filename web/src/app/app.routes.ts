import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  // Guest area
  {
    path: 'l/:slug',
    loadChildren: () =>
      import('./features/gift-list/gift-list.routes').then((m) => m.GIFT_LIST_ROUTES),
  },
  {
    path: 'r/:reservationId',
    loadChildren: () =>
      import('./features/reservation/reservation.routes').then((m) => m.RESERVATION_ROUTES),
  },
  // Organizer area
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register.component').then((m) => m.RegisterComponent),
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadChildren: () =>
      import('./features/dashboard/dashboard.routes').then((m) => m.DASHBOARD_ROUTES),
  },
  {
    path: 'lists/new',
    canActivate: [authGuard],
    loadComponent: () => import('./features/lists/list-new.component').then((m) => m.ListNewComponent),
  },
  {
    path: 'lists/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./features/lists/list-manage.component').then((m) => m.ListManageComponent),
  },
  {
    path: '',
    pathMatch: 'full',
    loadChildren: () =>
      import('./features/home/home.routes').then((m) => m.HOME_ROUTES),
  },
  { path: '**', redirectTo: '' },
];
