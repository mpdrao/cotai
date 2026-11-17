import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, shareReplay } from 'rxjs';
import { Event } from '../models/event.model';
import { Item } from '../models/gift-list.model';

/**
 * Events (US-07) and the items suggested for each one.
 * The catalog almost never changes, so the list is fetched once and reused.
 */
@Injectable({ providedIn: 'root' })
export class EventCatalogService {
  private readonly http = inject(HttpClient);
  private readonly events$ = this.http.get<Event[]>('/api/events').pipe(shareReplay(1));

  events(): Observable<Event[]> {
    return this.events$;
  }

  suggestedItems(eventCode: string): Observable<Item[]> {
    return this.http.get<Item[]>(`/api/events/${encodeURIComponent(eventCode)}/items`);
  }
}
