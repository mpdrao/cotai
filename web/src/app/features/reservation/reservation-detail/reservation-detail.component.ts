import { Component, OnDestroy, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { ReservationService } from '../services/reservation.service';
import { ReservationView } from '../../../core/models/reservation.model';

@Component({
  selector: 'app-reservation-detail',
  imports: [CurrencyPipe],
  template: `
    @if (view(); as r) {
      <h1>Minha reserva</h1>
      <p>{{ r.itemName }} × {{ r.quantity }} · {{ r.amountCents / 100 | currency: 'BRL' }}</p>
      <p>
        Status: <strong>{{ r.status }}</strong>
      </p>

      @if (r.status === 'PENDING' && r.payment.checkoutUrl) {
        <p>Pague até {{ r.expiresAt }}:</p>
        <img [src]="qrUrl(r.payment.qrCodePayload!)" width="220" height="220" alt="QR code de pagamento" />
        <p><a [href]="r.payment.checkoutUrl" target="_blank" rel="noopener">Abrir link de pagamento</a></p>
        <button (click)="cancel()">Cancelar reserva</button>
      } @else if (r.status === 'CONFIRMED') {
        <p>Pagamento confirmado. Obrigado!</p>
      }
    } @else if (error()) {
      <p>{{ error() }}</p>
    } @else {
      <p>Carregando…</p>
    }
  `,
})
export class ReservationDetailComponent implements OnDestroy {
  private readonly reservationService = inject(ReservationService);
  private readonly id = location.pathname.split('/').pop() ?? '';
  private readonly token = sessionStorage.getItem(`manage:${this.id}`) ?? '';
  private timer?: ReturnType<typeof setInterval>;

  protected readonly view = signal<ReservationView | null>(null);
  protected readonly error = signal('');

  constructor() {
    this.refresh();
    this.timer = setInterval(() => this.refresh(), 3000);
  }

  ngOnDestroy(): void {
    if (this.timer) {
      clearInterval(this.timer);
    }
  }

  protected qrUrl(payload: string): string {
    return `https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=${encodeURIComponent(payload)}`;
  }

  protected cancel(): void {
    this.reservationService.cancelReservation(this.id, this.token).subscribe(() => this.refresh());
  }

  private refresh(): void {
    this.reservationService.getReservation(this.id, this.token).subscribe({
      next: (v) => {
        this.view.set(v);
        if (v.status !== 'PENDING' && this.timer) {
          clearInterval(this.timer);
        }
      },
      error: () => this.error.set('Reserva não encontrada.'),
    });
  }
}
