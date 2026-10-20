import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { PublicGiftList } from '../../core/models/gift-list.model';
import { ReservationCreated, ReserveRequest } from '../../core/models/reservation.model';

@Injectable({ providedIn: 'root' })
export class GiftListService {
  private readonly api = inject(ApiService);

  getGiftList(slug: string): Observable<PublicGiftList> {
    return this.api.getGiftList(slug);
  }

  reserve(itemId: number, body: ReserveRequest, idempotencyKey: string): Observable<ReservationCreated> {
    return this.api.reserve(itemId, body, idempotencyKey);
  }
}
