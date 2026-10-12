import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { ReservationView } from '../../../core/models/reservation.model';

@Injectable({ providedIn: 'root' })
export class ReservationService {
  private readonly api = inject(ApiService);

  getReservation(id: string, manageToken: string): Observable<ReservationView> {
    return this.api.getReservation(id, manageToken);
  }

  cancelReservation(id: string, manageToken: string): Observable<void> {
    return this.api.cancelReservation(id, manageToken);
  }
}
