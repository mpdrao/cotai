import { Routes } from '@angular/router';

export const GIFT_LIST_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./gift-list-detail/gift-list-detail.component').then((m) => m.GiftListDetailComponent),
  },
];
