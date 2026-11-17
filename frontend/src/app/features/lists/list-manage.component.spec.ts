import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { Subject, of } from 'rxjs';
import { ListManageComponent } from './list-manage.component';
import { OrganizerService } from '../../core/services/organizer.service';
import { EventCatalogService } from '../../core/services/event-catalog.service';
import { GiftList } from '../../core/models/gift-list.model';

const LIST: GiftList = {
  id: 'l1',
  title: 'Churrasco da firma',
  slug: 'churrasco-da-firma',
  eventCode: 'BARBECUE',
  eventDate: '2099-12-01',
  status: 'DRAFT',
  reservationTtlMinutes: 30,
  items: [],
  createdAt: '2026-11-17T12:00:00Z',
};

describe('ListManageComponent', () => {
  it('keeps what the organizer typed in the list form when the items reload after an item change', () => {
    const reloads = new Subject<GiftList>();
    let calls = 0;
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: new Map([['id', 'l1']]) } } },
        { provide: EventCatalogService, useValue: { events: () => of([]) } },
        {
          provide: OrganizerService,
          // First call is the page load; the next ones are reloads after an item change.
          useValue: { get: () => (calls++ === 0 ? of(LIST) : reloads), summary: () => of(null) },
        },
      ],
    });
    const fixture = TestBed.createComponent(ListManageComponent);
    const component = fixture.componentInstance as unknown as {
      title: string;
      busy: { (): boolean; set(v: boolean): void };
      notice: () => string;
      afterChange(message: string): void;
    };
    fixture.detectChanges();
    expect(component.title).toBe('Churrasco da firma');

    component.busy.set(true);
    component.afterChange('Item adicionado.');
    component.title = 'Churrasco da firma 2026';

    expect(component.busy()).toBe(true);
    expect(component.notice()).toBe('');

    reloads.next({ ...LIST, items: [] });

    expect(component.title).toBe('Churrasco da firma 2026');
    expect(component.busy()).toBe(false);
    expect(component.notice()).toBe('Item adicionado.');
  });
});
