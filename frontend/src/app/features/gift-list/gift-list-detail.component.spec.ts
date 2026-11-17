import { TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';
import { GiftListDetailComponent } from './gift-list-detail.component';
import { GiftListService } from './gift-list.service';
import { PublicGiftList } from '../../core/models/gift-list.model';

const LIST: PublicGiftList = {
  title: 'Churrasco',
  eventTypeCode: 'BARBECUE',
  eventDate: '2026-12-01',
  status: 'PUBLISHED',
  acceptsReservations: true,
  items: [
    { id: 1, name: 'Carvão', unit: 'saco', unitPriceCents: 45_00, availableQuotas: 1 },
    { id: 2, name: 'Picanha', unit: 'kg', unitPriceCents: 90_00, availableQuotas: 0 },
  ],
};

describe('GiftListDetailComponent', () => {
  it('loads the list of the slug in the l/:slug route and counts reserved quotas', async () => {
    const requested: string[] = [];
    TestBed.configureTestingModule({
      providers: [
        provideRouter(
          [{ path: 'l/:slug', loadChildren: () => import('./gift-list.routes').then((m) => m.GIFT_LIST_ROUTES) }],
          withComponentInputBinding(),
        ),
        {
          provide: GiftListService,
          useValue: { getGiftList: (slug: string) => (requested.push(slug), of(LIST)), reserve: () => of() },
        },
      ],
    });

    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/l/churrasco-da-firma', GiftListDetailComponent);
    harness.detectChanges();

    expect(requested).toEqual(['churrasco-da-firma']);
    expect(harness.routeNativeElement?.textContent).toContain('1 de 2 cotas reservadas');
    expect(harness.routeNativeElement?.textContent).toContain('Todas as cotas foram reservadas');
  });
});
