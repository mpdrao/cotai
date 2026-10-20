import { Component, computed, inject, input, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { GiftListService } from './gift-list.service';
import { PublicGiftList, PublicItem } from '../../core/models/gift-list.model';
import { errorCode, errorMessage } from '../../core/copy/ui-copy';
import { CotaiButtonComponent, CotaiCardComponent, CotaiProgressComponent } from '../../shared/cotai-ui';

@Component({
  selector: 'app-gift-list-detail',
  imports: [CurrencyPipe, DatePipe, FormsModule, CotaiButtonComponent, CotaiCardComponent, CotaiProgressComponent],
  template: `
    @if (list(); as l) {
      <header class="list-header">
        <h1>{{ l.title }}</h1>
        <p class="meta">{{ l.eventDate | date: 'longDate' }}</p>
        <cotai-progress [value]="overall().percent" label="Cotas reservadas na lista" />
        <p class="meta">{{ overall().reserved }} de {{ overall().total }} cotas reservadas</p>
        @if (!l.acceptsReservations) {
          <p class="notice" role="status">Esta lista não está mais recebendo reservas.</p>
        }
      </header>

      @if (listNotice()) {
        <p class="notice" role="alert">{{ listNotice() }}</p>
      }

      @if (l.items.length === 0) {
        <cotai-card tinted><p class="empty">Esta lista ainda não tem itens.</p></cotai-card>
      }

      <ul class="items">
        @for (item of l.items; track item.id) {
          <li>
            <cotai-card>
              <div class="item">
                <div class="item-info">
                  <h2>{{ item.name }}</h2>
                  <p class="meta">{{ item.unit }} · {{ item.unitPriceCents / 100 | currency: 'BRL' }} por cota</p>
                  <cotai-progress
                    [value]="reservedPercent(item)"
                    [label]="'Cotas reservadas de ' + item.name" />
                  <p class="availability">
                    @if (item.availableQuotas === 0) {
                      Todas as cotas foram reservadas
                    } @else {
                      {{ item.availableQuotas }} de {{ item.totalQuotas }} cotas disponíveis
                    }
                  </p>
                </div>
                @if (selected()?.id !== item.id) {
                  <cotai-button
                    variant="secondary"
                    [fullWidth]="true"
                    [disabled]="!l.acceptsReservations || item.availableQuotas === 0"
                    (pressed)="select(item)">
                    Reservar minhas cotas
                  </cotai-button>
                }
              </div>

              @if (selected()?.id === item.id) {
                <form class="reserve" (ngSubmit)="reserve(item)" #form="ngForm">
                  <div class="field">
                    <span class="label" id="qty-label-{{ item.id }}">Quantidade de cotas</span>
                    <div class="stepper" role="group" [attr.aria-labelledby]="'qty-label-' + item.id">
                      <button type="button" aria-label="Diminuir" [disabled]="quantity() <= 1" (click)="step(-1, item)">−</button>
                      <output aria-live="polite">{{ quantity() }}</output>
                      <button type="button" aria-label="Aumentar" [disabled]="quantity() >= item.availableQuotas" (click)="step(1, item)">+</button>
                    </div>
                  </div>
                  <label class="field">
                    <span class="label">Seu nome</span>
                    <input class="cotai-input" name="guestName" autocomplete="name" required minlength="2" maxlength="80"
                           [(ngModel)]="guestName" />
                  </label>
                  <label class="field">
                    <span class="label">Seu e-mail</span>
                    <input class="cotai-input" name="guestEmail" type="email" inputmode="email" autocomplete="email" required
                           maxlength="254" [(ngModel)]="guestEmail" />
                  </label>
                  <p class="total">
                    Total: <strong>{{ (quantity() * item.unitPriceCents) / 100 | currency: 'BRL' }}</strong>
                  </p>
                  @if (error()) {
                    <p class="error" role="alert">{{ error() }}</p>
                  }
                  <div class="form-actions">
                    <cotai-button type="submit" [fullWidth]="true" [loading]="submitting()" [disabled]="!!form.invalid">
                      Ir para pagamento
                    </cotai-button>
                    <cotai-button variant="ghost" [fullWidth]="true" (pressed)="selected.set(null)">Voltar</cotai-button>
                  </div>
                </form>
              }
            </cotai-card>
          </li>
        }
      </ul>
    } @else if (notFound()) {
      <cotai-card tinted>
        <h1 class="state-title">Lista não encontrada</h1>
        <p>Confira se o link está completo.</p>
      </cotai-card>
    } @else {
      <p role="status">Carregando…</p>
    }
  `,
  styles: `
    .list-header { display: grid; gap: 0.5rem; margin-bottom: 1.5rem; }
    h1 { margin: 0; font-size: clamp(1.5rem, 1.1rem + 2vw, 2.25rem); font-weight: 800; line-height: 1.15; color: var(--cotai-primary); }
    .meta { margin: 0; color: var(--cotai-muted); font-size: 0.9375rem; }
    .notice { margin: 0 0 1rem; padding: 0.75rem 1rem; border-radius: var(--cotai-radius-sm); background: var(--cotai-warning-bg); color: var(--cotai-warning-ink); }
    .empty { margin: 0; }
    .items { display: grid; gap: 1rem; margin: 0; padding: 0; list-style: none; }
    .item { display: grid; gap: 1rem; }
    .item-info { display: grid; gap: 0.375rem; min-width: 0; }
    .item h2 { margin: 0; font-size: 1.0625rem; font-weight: 600; }
    .availability { margin: 0; font-size: 0.875rem; font-weight: 500; }
    .reserve { display: grid; gap: 1rem; margin-top: 1.25rem; padding-top: 1.25rem; border-top: 1px solid var(--cotai-border); }
    .field { display: grid; gap: 0.375rem; }
    .label { font-size: 0.875rem; font-weight: 600; }
    .stepper { display: inline-flex; align-items: center; gap: 0.5rem; }
    .stepper button {
      width: var(--cotai-tap-target); height: var(--cotai-tap-target);
      border: 1px solid var(--cotai-primary); border-radius: 50%;
      background: var(--cotai-surface); color: var(--cotai-primary); font-size: 1.25rem; cursor: pointer;
    }
    .stepper button:disabled { opacity: 0.45; cursor: not-allowed; }
    .stepper output { min-width: 2.5rem; text-align: center; font-weight: 700; font-size: 1.125rem; }
    .total { margin: 0; font-size: 1rem; }
    .error { margin: 0; padding: 0.75rem 1rem; border-radius: var(--cotai-radius-sm); background: var(--cotai-danger-bg); color: var(--cotai-danger-ink); }
    .form-actions { display: grid; gap: 0.5rem; }
    .state-title { font-size: 1.25rem; }
    @media (min-width: 40rem) {
      .item { grid-template-columns: minmax(0, 1fr) auto; align-items: end; }
      .item cotai-button { min-width: 14rem; }
    }
    @media (min-width: 64rem) {
      .items { grid-template-columns: repeat(2, minmax(0, 1fr)); align-items: start; }
      .item { grid-template-columns: minmax(0, 1fr); }
    }
  `,
})
export class GiftListDetailComponent {
  private readonly giftListService = inject(GiftListService);
  private readonly router = inject(Router);

  /** Bound from route param or fallback to location */
  readonly slug = input<string>();

  private readonly listSlug = location.pathname.split('/').pop() ?? '';

  protected readonly list = signal<PublicGiftList | null>(null);
  protected readonly notFound = signal(false);
  protected readonly selected = signal<PublicItem | null>(null);
  protected readonly quantity = signal(1);
  protected readonly submitting = signal(false);
  protected readonly error = signal('');
  protected readonly listNotice = signal('');

  protected readonly overall = computed(() => {
    const items = this.list()?.items ?? [];
    const total = items.reduce((sum, i) => sum + i.totalQuotas, 0);
    const reserved = items.reduce((sum, i) => sum + (i.totalQuotas - i.availableQuotas), 0);
    return { total, reserved, percent: total === 0 ? 0 : (reserved / total) * 100 };
  });

  protected guestName = '';
  protected guestEmail = '';

  constructor() {
    this.load();
  }

  protected reservedPercent(item: PublicItem): number {
    return item.totalQuotas === 0 ? 0 : ((item.totalQuotas - item.availableQuotas) / item.totalQuotas) * 100;
  }

  protected select(item: PublicItem): void {
    this.selected.set(item);
    this.quantity.set(1);
    this.error.set('');
    this.listNotice.set('');
  }

  protected step(delta: number, item: PublicItem): void {
    this.quantity.update((q) => Math.min(Math.max(q + delta, 1), item.availableQuotas));
  }

  protected reserve(item: PublicItem): void {
    const key = crypto.randomUUID();
    this.submitting.set(true);
    this.error.set('');
    this.giftListService
      .reserve(item.id, { guestName: this.guestName, guestEmail: this.guestEmail, quantity: this.quantity() }, key)
      .subscribe({
        next: (r) => {
          sessionStorage.setItem(`manage:${r.reservationId}`, r.manageToken);
          this.router.navigate(['/r', r.reservationId]);
        },
        error: (e) => {
          this.submitting.set(false);
          if (errorCode(e) === 'QUOTA_UNAVAILABLE') {
            // EC28: the availability on screen was stale. Close the form and reload the numbers.
            this.selected.set(null);
            this.listNotice.set(errorMessage(e));
            this.load();
          } else {
            this.error.set(errorMessage(e));
          }
        },
      });
  }

  private load(): void {
    this.giftListService.getGiftList(this.listSlug).subscribe({
      next: (l) => this.list.set(l),
      error: () => this.notFound.set(true),
    });
  }
}
