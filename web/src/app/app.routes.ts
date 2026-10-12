import { Routes } from '@angular/router';

export const routes: Routes = [
  // Guest area (no login)
  {
    path: 'l/:slug',
    loadComponent: () => import('./guest/gift-list.page').then((m) => m.GiftListPage),
  },
  {
    path: 'r/:reservationId',
    loadComponent: () => import('./guest/reservation.page').then((m) => m.ReservationPage),
  },
  // Organizer area
  {
    path: 'dashboard',
    loadComponent: () => import('./organizer/dashboard.page').then((m) => m.DashboardPage),
  },
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./home.page').then((m) => m.HomePage),
  },
  { path: '**', redirectTo: '' },
];
