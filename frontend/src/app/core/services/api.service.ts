import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PublicGiftList } from '../models/gift-list.model';
import { ReservationCreated, ReservationView, ReserveRequest } from '../models/reservation.model';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  getGiftList(slug: string): Observable<PublicGiftList> {
    return this.http.get<PublicGiftList>(`/api/public/gift-lists/${slug}`);
  }

  reserve(itemId: number, body: ReserveRequest, idempotencyKey: string): Observable<ReservationCreated> {
    return this.http.post<ReservationCreated>(`/api/public/items/${itemId}/reservations`, body, {
      headers: new HttpHeaders({ 'Idempotency-Key': idempotencyKey }),
    });
  }

  getReservation(id: string, manageToken: string): Observable<ReservationView> {
    return this.http.get<ReservationView>(`/api/public/reservations/${id}`, {
      headers: new HttpHeaders({ 'X-Manage-Token': manageToken }),
    });
  }

  cancelReservation(id: string, manageToken: string): Observable<void> {
    return this.http.delete<void>(`/api/public/reservations/${id}`, {
      headers: new HttpHeaders({ 'X-Manage-Token': manageToken }),
    });
  }
}
