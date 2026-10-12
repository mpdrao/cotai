import { Routes } from '@angular/router';

export const RESERVATION_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./reservation-detail/reservation-detail.component').then((m) => m.ReservationDetailComponent),
  },
];
