import { Component, computed, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ORGANIZER_COPY, errorMessage } from '../../core/copy/ui-copy';
import { EventType } from '../../core/models/organizer.model';
import { OrganizerService } from '../../core/services/organizer.service';
import { ItemEditorComponent } from './item-editor.component';
import { ItemDraft, ItemErrors, draftFrom, draftTotalCents, emptyDraft, toItemInput, validateDraft } from './item-draft';
import { CotaiButtonComponent, CotaiCardComponent } from '../../shared/cotai-ui';

/** UC02 + UC03: create a gift list from an event type and adjust the suggested items. */
@Component({
  selector: 'app-list-new',
  imports: [CurrencyPipe, FormsModule, RouterLink, ItemEditorComponent, CotaiButtonComponent, CotaiCardComponent],
  template: `
    <form class="page" (ngSubmit)="submit()" novalidate>
      <a routerLink="/dashboard" class="cotai-back">← Minhas listas</a>
      <h1 class="cotai-page-title">Criar minha lista</h1>

      <fieldset class="types">
        <legend class="cotai-label">Tipo de evento</legend>
        @if (eventTypes().length === 0) {
          <p role="status">Carregando tipos de evento…</p>
        }
        <div class="type-options">
          @for (type of eventTypes(); track type.code) {
            <label class="type" [class.type--selected]="selectedType()?.code === type.code">
              <input type="radio" name="eventType" [id]="'type-' + type.code" [value]="type.code"
                     [checked]="selectedType()?.code === type.code" (change)="chooseType(type)" />
              <span>{{ type.name }}</span>
            </label>
          }
        </div>
        @if (errors()['eventType']) { <p class="cotai-field-error">{{ errors()['eventType'] }}</p> }
      </fieldset>

      <cotai-card>
        <div class="details">
          <label class="cotai-field title">
            <span class="cotai-label">Nome da lista</span>
            <input class="cotai-input" id="list-title" name="title" maxlength="80" placeholder="Churrasco da firma"
                   [(ngModel)]="title" [attr.aria-invalid]="!!errors()['title']" />
            @if (errors()['title']) { <span class="cotai-field-error">{{ errors()['title'] }}</span> }
          </label>
          <label class="cotai-field">
            <span class="cotai-label">Data do evento</span>
            <input class="cotai-input" id="list-date" name="eventDate" type="date" [min]="today" [(ngModel)]="eventDate"
                   [attr.aria-invalid]="!!errors()['eventDate']" />
            @if (errors()['eventDate']) { <span class="cotai-field-error">{{ errors()['eventDate'] }}</span> }
          </label>
          <label class="cotai-field">
            <span class="cotai-label">Prazo para pagar (minutos)</span>
            <input class="cotai-input" id="list-ttl" name="ttl" type="number" inputmode="numeric" min="10" max="1440"
                   [(ngModel)]="ttl" aria-describedby="list-ttl-hint" [attr.aria-invalid]="!!errors()['ttl']" />
            <span class="cotai-hint" id="list-ttl-hint">Tempo que cada reserva fica guardada esperando o pagamento.</span>
            @if (errors()['ttl']) { <span class="cotai-field-error">{{ errors()['ttl'] }}</span> }
          </label>
        </div>
      </cotai-card>

      <section class="items" aria-labelledby="items-title">
        <div class="items-head">
          <h2 id="items-title">Itens</h2>
          <p class="cotai-hint">
            @if (selectedType()) {
              Sugestões para {{ selectedType()!.name }}. Mude o que quiser: nome, unidade, valor e número de cotas.
            } @else {
              Escolha o tipo de evento para receber sugestões, ou adicione os itens você mesmo.
            }
          </p>
        </div>
        @for (item of items(); track item; let i = $index) {
          <cotai-card>
            <div class="item">
              <app-item-editor [draft]="item" [errors]="itemErrors()[i] ?? {}" [prefix]="'new-item-' + i" (changed)="touched()" />
              <div class="item-foot">
                <span class="item-total">Total do item: <strong>{{ itemTotal(item) / 100 | currency: 'BRL' }}</strong></span>
                <cotai-button variant="ghost" (pressed)="removeItem(i)">Remover item</cotai-button>
              </div>
            </div>
          </cotai-card>
        }
        <cotai-button variant="secondary" [fullWidth]="true" [disabled]="items().length >= 100" (pressed)="addItem()">
          + Adicionar item
        </cotai-button>
      </section>

      <div class="footer">
        <p class="goal">Meta da lista: <strong>{{ goalCents() / 100 | currency: 'BRL' }}</strong> em {{ items().length }} itens</p>
        @if (error()) {
          <p class="cotai-alert cotai-alert--error" role="alert">{{ error() }}</p>
        }
        <cotai-button type="submit" [fullWidth]="true" [loading]="saving()">Criar minha lista</cotai-button>
        <p class="cotai-hint">A lista começa como rascunho. Você publica e compartilha o link quando estiver pronta.</p>
      </div>
    </form>
  `,
  styles: `
    .page { display: grid; gap: 1.25rem; }
    .types { margin: 0; padding: 0; border: 0; display: grid; gap: 0.5rem; min-width: 0; }
    .type-options { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0.5rem; }
    .type {
      position: relative; display: flex; align-items: center; justify-content: center; min-height: 3.25rem;
      padding: 0.5rem 0.75rem; border: 1px solid var(--cotai-border); border-radius: var(--cotai-radius-card);
      background: var(--cotai-surface); font-weight: 600; text-align: center; cursor: pointer;
    }
    .type input { position: absolute; opacity: 0; inset: 0; margin: 0; cursor: pointer; }
    .type:has(input:focus-visible) { outline: 3px solid var(--cotai-terracotta); outline-offset: 3px; }
    .type--selected { border: 2px solid var(--cotai-primary); background: var(--cotai-success-bg); color: var(--cotai-primary); }
    .details { display: grid; gap: 1rem; }
    .items { display: grid; gap: 0.75rem; }
    .items-head h2 { margin: 0; font-size: 1.25rem; color: var(--cotai-primary); }
    .items-head .cotai-hint { margin-top: 0.25rem; }
    .item { display: grid; gap: 0.5rem; }
    .item-foot { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 0.5rem; }
    .item-total { font-size: 0.875rem; color: var(--cotai-muted); }
    .footer { display: grid; gap: 0.75rem; padding-top: 0.5rem; border-top: 1px solid var(--cotai-border); }
    .goal { margin: 0; }
    @media (min-width: 40rem) {
      .type-options { grid-template-columns: repeat(4, minmax(0, 1fr)); }
      .details { grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); }
      .title { grid-column: 1 / -1; }
      .footer { justify-items: start; }
      .footer cotai-button { width: auto; min-width: 16rem; }
    }
  `,
})
export class ListNewComponent {
  private readonly organizer = inject(OrganizerService);
  private readonly router = inject(Router);

  protected readonly today = new Date().toLocaleDateString('en-CA');
  protected readonly eventTypes = signal<EventType[]>([]);
  protected readonly selectedType = signal<EventType | null>(null);
  protected readonly items = signal<ItemDraft[]>([]);
  protected readonly itemErrors = signal<ItemErrors[]>([]);
  protected readonly errors = signal<Record<string, string>>({});
  protected readonly error = signal('');
  protected readonly saving = signal(false);
  /** Bumped on every item edit so totals are recomputed (the editor mutates the drafts in place). */
  private readonly edits = signal(0);

  protected title = '';
  protected eventDate = '';
  protected ttl: number | null = 30;

  protected readonly goalCents = computed(() => {
    this.edits();
    return this.items().reduce((sum, d) => sum + draftTotalCents(d), 0);
  });

  constructor() {
    this.organizer.eventTypes().subscribe({
      next: (types) => this.eventTypes.set(types),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  protected touched(): void {
    this.edits.update((n) => n + 1);
  }

  protected chooseType(type: EventType): void {
    this.selectedType.set(type);
    this.ttl = type.defaultReservationTtlMinutes;
    this.organizer.templateItems(type.code).subscribe({
      next: (templates) => {
        this.items.set(
          templates.map((t) => draftFrom({ name: t.name, unit: t.unit, unitPriceCents: t.unitPriceCents, totalQuotas: t.suggestedQuotas })),
        );
        this.itemErrors.set([]);
      },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  protected itemTotal(d: ItemDraft): number {
    this.edits();
    return draftTotalCents(d);
  }

  protected addItem(): void {
    this.items.update((list) => [...list, emptyDraft()]);
  }

  protected removeItem(index: number): void {
    this.items.update((list) => list.filter((_, i) => i !== index));
    this.itemErrors.update((list) => list.filter((_, i) => i !== index));
  }

  protected submit(): void {
    const errors: Record<string, string> = {};
    if (!this.selectedType()) errors['eventType'] = 'Escolha o tipo de evento.';
    if (this.title.trim().length < 3) errors['title'] = 'Use pelo menos 3 caracteres.';
    if (!this.eventDate) errors['eventDate'] = 'Informe a data do evento.';
    else if (this.eventDate < this.today) errors['eventDate'] = 'A data do evento já passou.';
    if (this.ttl === null || this.ttl < 10 || this.ttl > 1440) errors['ttl'] = 'Use de 10 a 1440 minutos.';
    const itemErrors = this.items().map(validateDraft);
    this.errors.set(errors);
    this.itemErrors.set(itemErrors);
    const invalidItems = itemErrors.some((e) => Object.keys(e).length > 0);
    if (Object.keys(errors).length > 0 || invalidItems) {
      this.error.set('Confira os campos destacados.');
      return;
    }

    this.error.set('');
    this.saving.set(true);
    this.organizer
      .create({
        eventTypeCode: this.selectedType()!.code,
        title: this.title.trim(),
        eventDate: this.eventDate,
        reservationTtlMinutes: this.ttl!,
        items: this.items().map(toItemInput),
      })
      .subscribe({
        next: (list) => this.router.navigate(['/lists', list.id]),
        error: (e) => {
          this.saving.set(false);
          this.error.set(errorMessage(e, ORGANIZER_COPY));
        },
      });
  }
}
