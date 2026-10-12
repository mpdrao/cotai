import { Component, OnDestroy, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { PublicApi, ReservationView } from '../api/public-api';

/** Shows the QR code / link and polls every 3 s until the reservation leaves PENDING. */
@Component({
  selector: 'app-reservation-page',
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
        <p>Pagamento confirmado. Obrigado! 🎉</p>
      }
    } @else if (error()) {
      <p>{{ error() }}</p>
    } @else {
      <p>Carregando…</p>
    }
  `,
})
export class ReservationPage implements OnDestroy {
  private readonly api = inject(PublicApi);
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
    clearInterval(this.timer);
  }

  protected qrUrl(payload: string): string {
    // Placeholder renderer. TODO: render the QR client-side (e.g. `qrcode` package) instead of a 3rd party.
    return `https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=${encodeURIComponent(payload)}`;
  }

  protected cancel(): void {
    this.api.cancelReservation(this.id, this.token).subscribe(() => this.refresh());
  }

  private refresh(): void {
    this.api.getReservation(this.id, this.token).subscribe({
      next: (v) => {
        this.view.set(v);
        if (v.status !== 'PENDING') {
          clearInterval(this.timer);
        }
      },
      error: () => this.error.set('Reserva não encontrada.'),
    });
  }
}
