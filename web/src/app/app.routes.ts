import { Routes } from '@angular/router';

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
    path: 'dashboard',
    loadChildren: () =>
      import('./features/dashboard/dashboard.routes').then((m) => m.DASHBOARD_ROUTES),
  },
  {
    path: '',
    pathMatch: 'full',
    loadChildren: () =>
      import('./features/home/home.routes').then((m) => m.HOME_ROUTES),
  },
  { path: '**', redirectTo: '' },
];
