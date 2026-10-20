import { Component, OnDestroy, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { ReservationService } from './reservation.service';
import { ReservationView } from '../../core/models/reservation.model';
import { reservationCopy } from '../../core/copy/ui-copy';
import { CotaiButtonComponent, CotaiCardComponent, CotaiStatusBadgeComponent } from '../../shared/cotai-ui';

@Component({
  selector: 'app-reservation-detail',
  imports: [CurrencyPipe, DatePipe, CotaiButtonComponent, CotaiCardComponent, CotaiStatusBadgeComponent],
  template: `
    @if (view(); as r) {
      <div class="layout">
        <header class="head">
          <h1>Acompanhar minha reserva</h1>
          <div aria-live="polite">
            <cotai-status-badge [status]="copy()!.badge" />
            <p class="status-detail">{{ copy()!.detail }}</p>
          </div>
        </header>

        @if (r.status === 'PENDING' && r.payment.checkoutUrl) {
          <cotai-card>
            <h2>Finalize seu pagamento</h2>
            <p class="muted">Você tem até <strong>{{ r.expiresAt | date: 'shortTime' }}</strong> para pagar.</p>
            <img class="qr" [src]="qrUrl(r.payment.qrCodePayload!)" width="220" height="220"
                 alt="QR code com o link de pagamento" />
            <p class="muted small">Escaneie com outro aparelho ou abra o link neste celular.</p>
            <div class="pay-actions">
              <a class="cta" [href]="r.payment.checkoutUrl" target="_blank" rel="noopener">Ir para pagamento</a>
              <cotai-button variant="secondary" [fullWidth]="true" (pressed)="copyLink(r.payment.checkoutUrl!)">
                Copiar link de pagamento
              </cotai-button>
            </div>
            @if (copied()) {
              <p class="muted small" role="status">Link copiado.</p>
            }
          </cotai-card>
        }

        <cotai-card tinted>
          <h2>Detalhes da reserva</h2>
          <dl class="details">
            <div><dt>Item</dt><dd>{{ r.itemName }}</dd></div>
            <div><dt>Cotas</dt><dd>{{ r.quantity }}</dd></div>
            <div><dt>Total</dt><dd>{{ r.amountCents / 100 | currency: 'BRL' }}</dd></div>
            <div><dt>Válida até</dt><dd>{{ r.expiresAt | date: 'short' }}</dd></div>
          </dl>
          @if (r.status === 'PENDING') {
            <cotai-button variant="ghost" [fullWidth]="true" [loading]="cancelling()" (pressed)="cancel()">
              Cancelar reserva
            </cotai-button>
          }
        </cotai-card>
      </div>
    } @else if (error()) {
      <cotai-card tinted>
        <h1 class="state-title">{{ error() }}</h1>
        <p>Abra o link da reserva no mesmo navegador em que ela foi feita.</p>
      </cotai-card>
    } @else {
      <p role="status">Carregando…</p>
    }
  `,
  styles: `
    .layout { display: grid; gap: 1rem; }
    .head { display: grid; gap: 0.75rem; }
    h1 { margin: 0; font-size: clamp(1.5rem, 1.1rem + 2vw, 2.25rem); font-weight: 800; line-height: 1.15; color: var(--cotai-primary); }
    h2 { margin: 0 0 0.5rem; font-size: 1.125rem; }
    .status-detail { margin: 0.5rem 0 0; }
    .muted { margin: 0; color: var(--cotai-muted); }
    .small { font-size: 0.875rem; }
    .qr { display: block; margin: 1rem auto; padding: 0.5rem; background: var(--cotai-surface); border-radius: var(--cotai-radius-sm); max-width: 100%; height: auto; }
    .pay-actions { display: grid; gap: 0.5rem; margin-top: 1rem; }
    .cta {
      display: flex; align-items: center; justify-content: center;
      min-height: var(--cotai-tap-target); padding: 0.7rem 1.25rem;
      border-radius: var(--cotai-radius-pill); background: var(--cotai-primary); color: var(--cotai-surface);
      font-weight: 600; text-decoration: none;
    }
    .details { display: grid; gap: 0.5rem; margin: 0 0 0.75rem; }
    .details div { display: flex; justify-content: space-between; gap: 1rem; }
    .details dt { color: var(--cotai-muted); }
    .details dd { margin: 0; font-weight: 600; text-align: right; }
    .state-title { font-size: 1.25rem; }
    @media (min-width: 48rem) {
      .layout { grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr); align-items: start; }
      .head { grid-column: 1 / -1; }
    }
  `,
})
export class ReservationDetailComponent implements OnDestroy {
  private readonly reservationService = inject(ReservationService);
  private readonly id = location.pathname.split('/').pop() ?? '';
  private readonly token = sessionStorage.getItem(`manage:${this.id}`) ?? '';
  private timer?: ReturnType<typeof setInterval>;

  protected readonly view = signal<ReservationView | null>(null);
  protected readonly copy = computed(() => {
    const v = this.view();
    return v ? reservationCopy(v) : null;
  });
  protected readonly error = signal('');
  protected readonly cancelling = signal(false);
  protected readonly copied = signal(false);

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

  protected copyLink(url: string): void {
    navigator.clipboard?.writeText(url).then(() => this.copied.set(true));
  }

  protected cancel(): void {
    this.cancelling.set(true);
    this.reservationService.cancelReservation(this.id, this.token).subscribe({
      next: () => this.refresh(),
      error: () => this.refresh(),
      complete: () => this.cancelling.set(false),
    });
  }

  private refresh(): void {
    this.reservationService.getReservation(this.id, this.token).subscribe({
      next: (v) => {
        this.view.set(v);
        if (v.status !== 'PENDING' && this.timer) {
          clearInterval(this.timer);
        }
      },
      error: () => this.error.set('Reserva não encontrada'),
    });
  }
}
