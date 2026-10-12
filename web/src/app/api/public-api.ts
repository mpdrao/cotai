import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

// NOTE: hand-written for now. Planned: generate these types from docs/api/openapi.yaml.

export interface PublicItem {
  id: number;
  name: string;
  unit: string;
  unitPriceCents: number;
  totalQuotas: number;
  availableQuotas: number;
}

export interface PublicGiftList {
  title: string;
  eventTypeCode: string;
  eventDate: string;
  status: 'DRAFT' | 'PUBLISHED' | 'CLOSED';
  acceptsReservations: boolean;
  items: PublicItem[];
}

export interface PaymentInfo {
  paymentId: string;
  status: string;
  amountCents: number;
  providerReference: string | null;
  checkoutUrl: string | null;
  qrCodePayload: string | null;
  expiresAt: string;
}

export interface ReservationCreated {
  reservationId: string;
  manageToken: string;
  status: string;
  quantity: number;
  amountCents: number;
  expiresAt: string;
  payment: PaymentInfo;
}

export interface ReservationView {
  reservationId: string;
  itemName: string;
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'EXPIRED';
  quantity: number;
  amountCents: number;
  expiresAt: string;
  payment: PaymentInfo;
}

export interface ReserveRequest {
  guestName: string;
  guestEmail: string;
  quantity: number;
}

@Injectable({ providedIn: 'root' })
export class PublicApi {
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
