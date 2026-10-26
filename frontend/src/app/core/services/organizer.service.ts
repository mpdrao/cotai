import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CreateGiftListRequest,
  EventType,
  GiftList,
  GiftListSummary,
  GiftListSummaryReport,
  Item,
  ItemInput,
  TemplateItem,
  UpdateGiftListRequest,
} from '../models/organizer.model';

/** Organizer endpoints. The bearer token is added by authInterceptor. */
@Injectable({ providedIn: 'root' })
export class OrganizerService {
  private readonly http = inject(HttpClient);

  eventTypes(): Observable<EventType[]> {
    return this.http.get<EventType[]>('/api/event-types');
  }

  templateItems(eventTypeCode: string): Observable<TemplateItem[]> {
    return this.http.get<TemplateItem[]>(`/api/event-types/${eventTypeCode}/template-items`);
  }

  myLists(): Observable<GiftListSummary[]> {
    return this.http.get<GiftListSummary[]>('/api/gift-lists');
  }

  create(body: CreateGiftListRequest): Observable<GiftList> {
    return this.http.post<GiftList>('/api/gift-lists', body);
  }

  get(id: string): Observable<GiftList> {
    return this.http.get<GiftList>(`/api/gift-lists/${id}`);
  }

  update(id: string, body: UpdateGiftListRequest): Observable<GiftList> {
    return this.http.patch<GiftList>(`/api/gift-lists/${id}`, body);
  }

  publish(id: string): Observable<GiftList> {
    return this.http.post<GiftList>(`/api/gift-lists/${id}/publish`, null);
  }

  close(id: string): Observable<GiftList> {
    return this.http.post<GiftList>(`/api/gift-lists/${id}/close`, null);
  }

  addItem(id: string, item: ItemInput): Observable<Item> {
    return this.http.post<Item>(`/api/gift-lists/${id}/items`, item);
  }

  updateItem(id: string, itemId: number, item: ItemInput): Observable<Item> {
    return this.http.put<Item>(`/api/gift-lists/${id}/items/${itemId}`, item);
  }

  uploadPhoto(id: string, itemId: number, file: File): Observable<Item> {
    const body = new FormData();
    body.append('file', file);
    return this.http.put<Item>(`/api/gift-lists/${id}/items/${itemId}/photo`, body);
  }

  removePhoto(id: string, itemId: number): Observable<Item> {
    return this.http.delete<Item>(`/api/gift-lists/${id}/items/${itemId}/photo`);
  }

  archiveItem(id: string, itemId: number): Observable<void> {
    return this.http.delete<void>(`/api/gift-lists/${id}/items/${itemId}`);
  }

  summary(id: string): Observable<GiftListSummaryReport> {
    return this.http.get<GiftListSummaryReport>(`/api/gift-lists/${id}/summary`);
  }
}
