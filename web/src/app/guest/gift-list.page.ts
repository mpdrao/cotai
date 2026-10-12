import { Component, inject, input, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { PublicApi, PublicGiftList, PublicItem } from '../api/public-api';

@Component({
  selector: 'app-gift-list-page',
  imports: [CurrencyPipe, FormsModule],
  template: `
    @if (list(); as l) {
      <h1>{{ l.title }}</h1>
      <p>{{ l.eventDate }} · {{ l.status }}</p>

      @for (item of l.items; track item.id) {
        <section class="item">
          <div>
            <strong>{{ item.name }}</strong> <small>({{ item.unit }})</small><br />
            {{ item.unitPriceCents / 100 | currency: 'BRL' }} por cota ·
            {{ item.availableQuotas }} de {{ item.totalQuotas }} disponíveis
          </div>
          <button [disabled]="!l.acceptsReservations || item.availableQuotas === 0" (click)="select(item)">
            Reservar
          </button>
        </section>
      }

      @if (selected(); as s) {
        <form class="reserve" (ngSubmit)="reserve(s)">
          <h2>Reservar: {{ s.name }}</h2>
          <input name="name" placeholder="Seu nome" [(ngModel)]="guestName" required minlength="2" />
          <input name="email" type="email" placeholder="Seu e-mail" [(ngModel)]="guestEmail" required />
          <input name="qty" type="number" min="1" [max]="s.availableQuotas" [(ngModel)]="quantity" required />
          <button type="submit">Ir para o pagamento</button>
          @if (error()) {
            <p class="error">{{ error() }}</p>
          }
        </form>
      }
    } @else if (notFound()) {
      <p>Lista não encontrada.</p>
    } @else {
      <p>Carregando…</p>
    }
  `,
  styles: `
    .item { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid #e5e7eb; }
    .reserve { display: grid; gap: 8px; margin-top: 24px; max-width: 360px; }
    .error { color: #b91c1c; }
  `,
})
export class GiftListPage {
  private readonly api = inject(PublicApi);
  private readonly router = inject(Router);

  /** Bound from the route param via withComponentInputBinding or read below. */
  readonly slug = input<string>();

  protected readonly list = signal<PublicGiftList | null>(null);
  protected readonly notFound = signal(false);
  protected readonly selected = signal<PublicItem | null>(null);
  protected readonly error = signal('');

  protected guestName = '';
  protected guestEmail = '';
  protected quantity = 1;

  constructor() {
    const slug = location.pathname.split('/').pop() ?? '';
    this.api.getGiftList(slug).subscribe({
      next: (l) => this.list.set(l),
      error: () => this.notFound.set(true),
    });
  }

  protected select(item: PublicItem): void {
    this.selected.set(item);
    this.quantity = 1;
    this.error.set('');
  }

  protected reserve(item: PublicItem): void {
    // One key per submit attempt: a double click or a network retry must not create two reservations.
    const key = crypto.randomUUID();
    this.api
      .reserve(item.id, { guestName: this.guestName, guestEmail: this.guestEmail, quantity: this.quantity }, key)
      .subscribe({
        next: (r) => {
          sessionStorage.setItem(`manage:${r.reservationId}`, r.manageToken);
          this.router.navigate(['/r', r.reservationId]);
        },
        error: (e) => this.error.set(e.error?.detail ?? 'Não foi possível reservar.'),
      });
  }
}
