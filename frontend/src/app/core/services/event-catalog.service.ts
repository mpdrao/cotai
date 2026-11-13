import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, shareReplay } from 'rxjs';
import { EventType, TemplateItem } from '../models/event-type.model';

/**
 * Event types (US-07) and the items suggested for each one.
 * The catalog almost never changes, so the list is fetched once and reused.
 */
@Injectable({ providedIn: 'root' })
export class EventCatalogService {
  private readonly http = inject(HttpClient);
  private readonly types$ = this.http.get<EventType[]>('/api/event-types').pipe(shareReplay(1));

  eventTypes(): Observable<EventType[]> {
    return this.types$;
  }

  suggestedItems(eventTypeCode: string): Observable<TemplateItem[]> {
    return this.http.get<TemplateItem[]>(`/api/event-types/${encodeURIComponent(eventTypeCode)}/template-items`);
  }
}
