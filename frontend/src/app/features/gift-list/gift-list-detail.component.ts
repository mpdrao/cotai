import { Component, ElementRef, Injector, afterNextRender, computed, inject, input, signal, viewChild } from '@angular/core';
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
        <div class="event-heading">
          <p class="cotai-eyebrow">Junta pra fazer acontecer.</p>
          <h1>{{ l.title }}</h1>
          <p class="meta">{{ l.eventDate | date: 'longDate' }}</p>
        </div>
        <div class="event-progress">
          <p><strong>{{ overall().reserved }}</strong> de {{ overall().total }} cotas reservadas</p>
          <cotai-progress [value]="overall().percent" label="Cotas reservadas na lista" />
          <p class="meta">Cada cota faz parte do encontro.</p>
        </div>
        @if (!l.acceptsReservations) {
          <p class="notice" role="status">Esta lista não está mais recebendo reservas.</p>
        }
      </header>

      @if (listNotice()) {
        <p #listNoticeEl class="notice" role="alert" tabindex="-1">{{ listNotice() }}</p>
      }

      @if (l.items.length === 0) {
        <cotai-card tinted><p class="empty">Esta lista ainda não tem itens.</p></cotai-card>
      }

      @if (l.items.length > 0) {
        <div class="collection-heading">
          <h2>{{ l.acceptsReservations ? 'Escolha como contribuir' : 'Itens do encontro' }}</h2>
          @if (l.acceptsReservations) {
            <p class="meta">Reserve suas cotas e finalize o pagamento dentro do prazo.</p>
          }
        </div>
      }
      <ul class="items" #itemsList>
        @for (item of l.items; track item.id) {
          <li [attr.data-item-id]="item.id">
            <cotai-card [elevation]="selected()?.id === item.id ? 'soft' : 'flat'" [class.selected]="selected()?.id === item.id">
              <div class="item">
                <div class="item-info">
                  <h2>{{ item.name }}</h2>
                  <p class="meta">{{ item.unit }}</p>
                  <p class="price"><strong>{{ item.unitPriceCents / 100 | currency: 'BRL' }}</strong> <span>por cota</span></p>
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
                <form #reservationForm class="reserve" (ngSubmit)="reserve(item)" #form="ngForm">
                  <h3 tabindex="-1">Suas cotas para este encontro</h3>
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
                    <cotai-button variant="ghost" [fullWidth]="true" (pressed)="closeReservation()">Voltar</cotai-button>
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
    .list-header { display: grid; gap: 2rem; margin-bottom: 2.5rem; padding: 1.5rem; border-radius: var(--cotai-radius-lg); background: var(--cotai-surface-sage); }
    .event-heading { display: grid; gap: .75rem; min-width: 0; }
    .event-progress { display: grid; gap: .75rem; align-content: center; }
    .event-progress p { margin: 0; }
    .event-progress strong { font-size: 1.5rem; }
    .collection-heading { margin-bottom: 1.5rem; }
    .collection-heading h2 { font-size: 1.25rem; margin: 0 0 .5rem; }
    .selected { --cotai-card-border: var(--cotai-primary); }
    .price { display: flex; align-items: baseline; flex-wrap: wrap; gap: .375rem; margin: .5rem 0 .75rem; }
    .price strong { font-size: 1.625rem; letter-spacing: -.025em; }
    .price span { color: var(--cotai-muted); font-size: .875rem; }
    .reserve h3 { font-size: 1rem; margin: 0; }
    .list-header .notice { grid-column: 1 / -1; margin: 0; }
    h1 { margin: 0; font-size: clamp(1.5rem, 1.1rem + 2vw, 2.25rem); font-weight: 800; line-height: 1.15; color: var(--cotai-primary); overflow-wrap: anywhere; }
    .meta { margin: 0; color: var(--cotai-muted); font-size: 0.9375rem; }
    .notice { margin: 0 0 1rem; padding: 0.75rem 1rem; border-radius: var(--cotai-radius-sm); background: var(--cotai-warning-bg); color: var(--cotai-warning-ink); }
    .empty { margin: 0; }
    .items { display: grid; grid-template-columns: minmax(0, 1fr); gap: 1rem; margin: 0; padding: 0; list-style: none; }
    .item { display: grid; gap: 1rem; }
    .item-info { display: grid; gap: .5rem; min-width: 0; }
    .item h2 { margin: 0; font-size: 1.25rem; line-height: 1.3; font-weight: 600; }
    .availability { margin: 0; font-size: 0.875rem; font-weight: 500; }
    .reserve { display: grid; gap: 1.5rem; margin-top: 1.5rem; padding-top: 1.5rem; border-top: 1px solid var(--cotai-border); }
    .field { display: grid; gap: .5rem; min-width: 0; }
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
    .total strong { font-size: 1.5rem; }
    .error { margin: 0; padding: 0.75rem 1rem; border-radius: var(--cotai-radius-sm); background: var(--cotai-danger-bg); color: var(--cotai-danger-ink); }
    .form-actions { display: grid; gap: 0.5rem; }
    .state-title { font-size: 1.25rem; }
    @media (min-width: 40rem) {
      .item { grid-template-columns: minmax(0, 1fr) auto; align-items: end; }
      .item cotai-button { min-width: 14rem; }
    }
    @media (min-width: 64rem) {
      .list-header { grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr); padding: 2.5rem; gap: 3rem; }
      .items { grid-template-columns: repeat(2, minmax(0, 1fr)); align-items: start; }
      .item { grid-template-columns: minmax(0, 1fr); }
    }
  `,
})
export class GiftListDetailComponent {
  private readonly injector = inject(Injector);
  private readonly reservationForm = viewChild<ElementRef<HTMLFormElement>>('reservationForm');
  private readonly itemsList = viewChild<ElementRef<HTMLUListElement>>('itemsList');
  private readonly listNoticeEl = viewChild<ElementRef<HTMLParagraphElement>>('listNoticeEl');
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
    afterNextRender(() => this.reservationForm()?.nativeElement.querySelector('h3')?.focus(), { injector: this.injector });
  }

  protected closeReservation(): void {
    const id = this.selected()?.id;
    this.selected.set(null);
    afterNextRender(() => this.itemsList()?.nativeElement.querySelector<HTMLButtonElement>(`[data-item-id="${id}"] button`)?.focus(), { injector: this.injector });
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
            // The form held focus; move it to the notice (the item button may come back disabled).
            afterNextRender(() => this.listNoticeEl()?.nativeElement.focus(), { injector: this.injector });
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
