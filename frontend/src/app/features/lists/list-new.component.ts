import { Component, computed, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { ORGANIZER_COPY, errorMessage } from '../../core/copy/ui-copy';
import { EventType } from '../../core/models/organizer.model';
import { OrganizerService } from '../../core/services/organizer.service';
import { ItemEditorComponent } from './item-editor.component';
import { ItemDraft, ItemErrors, draftFrom, draftTotalCents, emptyDraft, toItemInput, validateDraft } from './item-draft';
import { CotaiButtonComponent, CotaiCardComponent } from '../../shared/cotai-ui';

/** Ideas for "Nome da lista", by event type code. Unknown codes fall back to OTHER. */
const TITLE_SUGGESTIONS: Record<string, string[]> = {
  BARBECUE: ['Churrasco da firma', 'Churras de fim de ano', 'Churrasco de aniversário', 'Resenha do fim de semana'],
  BABY_SHOWER: ['Chá de bebê da Ana', 'Chá de fraldas', 'Chá revelação', 'Chegada do bebê'],
  WEDDING: ['Casamento de Ana e Pedro', 'Nosso casamento', 'Lua de mel dos noivos', 'Casa nova dos noivos'],
  OTHER: ['Aniversário de 30 anos', 'Bodas de prata', 'Formatura da turma', 'Vaquinha da galera'],
};

function titleSuggestionsFor(type: EventType | null): string[] {
  return type ? (TITLE_SUGGESTIONS[type.code] ?? TITLE_SUGGESTIONS['OTHER']) : [];
}

/** UC02 + UC03: create a gift list from an event type and adjust the suggested items. */
@Component({
  selector: 'app-list-new',
  imports: [CurrencyPipe, FormsModule, RouterLink, ItemEditorComponent, CotaiButtonComponent, CotaiCardComponent],
  template: `
    <form class="page" (ngSubmit)="submit()" novalidate>
      <header class="intro">
        <a routerLink="/dashboard" class="cotai-back">← Minhas listas</a>
        <h1 class="cotai-page-title">
          Criar minha
          <span class="marked">lista<svg class="stroke" viewBox="0 0 120 12" preserveAspectRatio="none" aria-hidden="true"><path d="M3 8.5c26-4.5 62-6 114-3.5" /></svg></span>
        </h1>
        <p class="cotai-lead">Escolha o tipo de evento que a gente já sugere os itens. Dá pra mudar tudo antes de publicar.</p>
      </header>

      <fieldset class="types">
        <legend class="section-title">Que tipo de evento?</legend>
        @if (eventTypes().length === 0) {
          <p class="cotai-hint" role="status">Carregando tipos de evento…</p>
        }
        <div class="type-options">
          @for (type of eventTypes(); track type.code) {
            <label class="type" [class.type--selected]="selectedType()?.code === type.code">
              <input type="radio" name="eventType" [id]="'type-' + type.code" [value]="type.code"
                     [checked]="selectedType()?.code === type.code" (change)="chooseType(type)" />
              <svg class="type-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75"
                   stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                @switch (type.code) {
                  @case ('BARBECUE') {
                    <path d="M5 9h14a7 7 0 0 1-14 0ZM8.5 15.2 6.5 21M15.5 15.2l2 5.8M12 16v5" />
                    <path class="accent" d="M9 2.5c-.7.9-.7 1.9 0 2.8M12 2c-.7.9-.7 1.9 0 2.8M15 2.5c-.7.9-.7 1.9 0 2.8" />
                  }
                  @case ('BABY_SHOWER') {
                    <path d="M10.5 5.5c0-1.7.7-3 1.5-3s1.5 1.3 1.5 3M9 5.5h6V8H9ZM9.5 8h5l.5 2v9.5a1.5 1.5 0 0 1-1.5 1.5h-3A1.5 1.5 0 0 1 9 19.5V10Z" />
                    <path class="accent" d="M11.5 12.5H13M11.5 15.5H13" />
                  }
                  @case ('WEDDING') {
                    <g transform="rotate(-12 7.5 20)"><path d="M5 5h5l-.4 4.5a2.1 2.1 0 0 1-4.2 0ZM7.5 11.6V20M5.5 20h4" /></g>
                    <g transform="rotate(12 16.5 20)"><path d="M14 5h5l-.4 4.5a2.1 2.1 0 0 1-4.2 0ZM16.5 11.6V20M14.5 20h4" /></g>
                    <path class="accent" d="M12 1.5v2.2M9.8 2.6l.9 1.2M14.2 2.6l-.9 1.2" />
                  }
                  @default {
                    <path d="M5.8 11.3 2 22l10.7-3.8M11 13c1.9 1.9 2.8 4.2 2 5-.8.8-3.1-.1-5-2s-2.8-4.2-2-5c.8-.8 3.1.1 5 2Z" />
                    <path class="accent" d="m22 2-2.2.8a2.9 2.9 0 0 0-2 3.1c.1.9-.6 1.6-1.4 1.6H16c-.9 0-1.6.6-1.8 1.4L14 10M22 13l-.8-.3c-.9-.3-1.8.2-2 1.1-.1.7-.7 1.2-1.4 1.2H17M11 2l.3.8c.3.9-.2 1.8-1.1 2-.7.1-1.2.7-1.2 1.4V7" />
                  }
                }
              </svg>
              <span>{{ type.name }}</span>
            </label>
          }
        </div>
        @if (errors()['eventType']) { <p class="cotai-field-error">{{ errors()['eventType'] }}</p> }
      </fieldset>

      <section class="panel" aria-labelledby="event-details-title">
        <h2 class="section-title" id="event-details-title">Sobre o encontro</h2>
        <div class="details">
          <div class="cotai-field title">
            <label class="cotai-label" for="list-title">Nome da lista</label>
            <input class="cotai-input" id="list-title" name="title" maxlength="80" [placeholder]="titlePlaceholder()"
                   [(ngModel)]="title" [attr.aria-invalid]="!!errors()['title']" />
            @if (titleSuggestions().length > 0) {
              <div class="suggestions" role="group" aria-labelledby="title-suggestions-label">
                <span class="cotai-hint" id="title-suggestions-label">Sem ideia? Toque em uma:</span>
                @for (suggestion of titleSuggestions(); track suggestion) {
                  <button type="button" class="suggestion" [attr.aria-pressed]="title === suggestion" (click)="title = suggestion">
                    {{ suggestion }}
                  </button>
                }
              </div>
            }
            @if (errors()['title']) { <span class="cotai-field-error">{{ errors()['title'] }}</span> }
          </div>
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
      </section>

      <section class="items" aria-labelledby="items-title">
        <div class="items-head">
          <h2 class="section-title" id="items-title">Itens</h2>
          @if (items().length > 0) {
            <p class="items-goal">{{ goalCents() / 100 | currency: 'BRL' }} <span>em {{ items().length }} {{ items().length === 1 ? 'item' : 'itens' }}</span></p>
          }
          <p class="cotai-hint items-hint">
            @if (selectedType()) {
              Sugestões para {{ selectedType()!.name }}. Mude o que quiser: nome, unidade, valor e número de cotas.
            } @else {
              Escolha o tipo de evento para receber sugestões, ou adicione os itens você mesmo.
            }
          </p>
        </div>
        @if (loadingItems()) {
          <p class="cotai-hint" role="status">Carregando sugestões…</p>
        }
        @for (item of items(); track item; let i = $index) {
          <cotai-card>
            <div class="item">
              <div class="item-head">
                <span class="item-index">Item {{ i + 1 }}</span>
                <span class="item-total">{{ itemTotal(item) / 100 | currency: 'BRL' }}</span>
                <cotai-button variant="ghost" (pressed)="removeItem(i)">Remover</cotai-button>
              </div>
              <app-item-editor [draft]="item" [errors]="itemErrors()[i] ?? {}" [prefix]="'new-item-' + i" (changed)="touched()" />
            </div>
          </cotai-card>
        }
        <cotai-button variant="secondary" [fullWidth]="true" [disabled]="items().length >= 100" (pressed)="addItem()">
          + Adicionar item
        </cotai-button>
      </section>

      <div class="footer">
        <p class="goal">Meta da lista: <strong>{{ goalCents() / 100 | currency: 'BRL' }}</strong> em {{ items().length }} {{ items().length === 1 ? 'item' : 'itens' }}</p>
        @if (error()) {
          <p class="cotai-alert cotai-alert--error" role="alert">{{ error() }}</p>
        }
        <cotai-button type="submit" [fullWidth]="true" [loading]="saving()">Criar minha lista</cotai-button>
        <p class="cotai-hint">A lista começa como rascunho. Você publica e compartilha o link quando estiver pronta.</p>
      </div>
    </form>
  `,
  styles: `
    .page { display: grid; gap: 2rem; max-width: 56rem; margin-inline: auto; }

    .intro { display: grid; gap: 0.5rem; }
    .intro .cotai-back { justify-self: start; }
    .intro .cotai-lead { margin: 0; max-width: 34rem; }
    /* Hand-drawn brush stroke under one word, echoing the stroke in the approved identity board. */
    .marked { position: relative; display: inline-block; }
    .stroke {
      position: absolute; left: -0.1em; right: -0.15em; bottom: -0.2em; width: calc(100% + 0.25em); height: 0.4em;
      fill: none; stroke: var(--cotai-terracotta); stroke-width: 4; stroke-linecap: round; overflow: visible;
    }

    .section-title { margin: 0; padding: 0; font-size: 1.25rem; font-weight: 700; color: var(--cotai-ink); }

    .types { display: grid; gap: 1rem; min-width: 0; margin: 0; padding: 0; border: 0; }
    .type-options { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0.75rem; }
    .type {
      position: relative; display: grid; justify-items: center; align-content: center; gap: 0.5rem; min-height: 6.5rem;
      padding: 1rem 0.5rem; border: 1px solid var(--cotai-border); border-radius: var(--cotai-radius-card);
      background: var(--cotai-surface); font-weight: 600; text-align: center; cursor: pointer;
      transition: border-color var(--cotai-motion), background var(--cotai-motion);
    }
    .type:hover { border-color: var(--cotai-secondary); }
    .type input { position: absolute; opacity: 0; inset: 0; margin: 0; cursor: pointer; }
    .type:has(input:focus-visible) { outline: 3px solid var(--cotai-primary); outline-offset: 3px; }
    .type-icon { width: 2.25rem; height: 2.25rem; color: var(--cotai-primary); }
    .type-icon .accent { stroke: var(--cotai-terracotta); }
    /* Selection is shown by a thicker border and tint, not by color alone. */
    .type--selected { border-color: var(--cotai-primary); box-shadow: inset 0 0 0 1px var(--cotai-primary); background: var(--cotai-surface-sage); color: var(--cotai-primary); }

    .panel {
      display: grid; gap: 1.5rem; min-width: 0; padding: 1.25rem;
      border: 1px solid var(--cotai-border); border-radius: var(--cotai-radius-card); background: var(--cotai-surface);
    }
    .details { display: grid; gap: 1.5rem; }
    .suggestions { display: flex; flex-wrap: wrap; align-items: center; gap: 0.5rem; }
    .suggestions .cotai-hint { flex-basis: 100%; }
    .suggestion {
      padding: 0.5rem 0.875rem; border: 1px solid var(--cotai-border); border-radius: var(--cotai-radius-pill);
      background: var(--cotai-background); color: var(--cotai-ink); font-size: 0.875rem; cursor: pointer;
      transition: border-color var(--cotai-motion), background var(--cotai-motion);
    }
    .suggestion:hover { border-color: var(--cotai-secondary); }
    .suggestion[aria-pressed='true'] { border-color: var(--cotai-primary); background: var(--cotai-surface-sage); color: var(--cotai-primary); font-weight: 600; }

    .items { display: grid; gap: 1rem; }
    .items-head { display: flex; flex-wrap: wrap; align-items: baseline; justify-content: space-between; gap: 0.25rem 1rem; }
    .items-goal { margin: 0; font-weight: 700; color: var(--cotai-primary); }
    .items-goal span { font-weight: 400; color: var(--cotai-muted); font-size: 0.875rem; }
    .items-hint { flex-basis: 100%; }
    .items cotai-card { --cotai-card-padding: 1rem 1.25rem 1.25rem; }
    .item { display: grid; grid-template-columns: minmax(0, 1fr); gap: 0.75rem; }
    .item-head { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 0 0.75rem; }
    .item-head > * { overflow-wrap: normal; white-space: nowrap; }
    .item-index { font-size: 0.875rem; font-weight: 600; color: var(--cotai-muted); }
    .item-total { font-weight: 700; }
    .item-head cotai-button { margin-left: auto; }

    .footer { display: grid; gap: 0.75rem; padding-top: 2rem; border-top: 1px solid var(--cotai-border); }
    .goal { margin: 0; }

    @media (min-width: 40rem) {
      .page { gap: 2.5rem; }
      .type-options { grid-template-columns: repeat(4, minmax(0, 1fr)); }
      .panel { padding: 2rem; }
      .details { grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); }
      .title { grid-column: 1 / -1; }
      .items cotai-card { --cotai-card-padding: 1.25rem 1.5rem 1.5rem; }
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
  protected readonly loadingItems = signal(false);
  protected readonly titleSuggestions = computed(() => titleSuggestionsFor(this.selectedType()));
  protected readonly titlePlaceholder = computed(() => `Ex.: ${this.titleSuggestions()[0] ?? 'Churrasco da firma'}`);
  /** Bumped on every item edit so totals are recomputed (the editor mutates the drafts in place). */
  private readonly edits = signal(0);
  /** Pending template request; cancelled when another type is picked so a slow, stale response can't win. */
  private templateRequest?: Subscription;

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
    // A title picked from the previous type's ideas is swapped for the matching idea of the new type; typed titles stay.
    const previousIndex = this.titleSuggestions().indexOf(this.title);
    this.selectedType.set(type);
    if (previousIndex >= 0) this.title = this.titleSuggestions()[previousIndex] ?? this.titleSuggestions()[0];
    this.ttl = type.defaultReservationTtlMinutes;
    this.loadingItems.set(true);
    this.templateRequest?.unsubscribe();
    this.templateRequest = this.organizer.templateItems(type.code).subscribe({
      next: (templates) => {
        this.loadingItems.set(false);
        this.items.set(
          templates.map((t) => draftFrom({ name: t.name, unit: t.unit, unitPriceCents: t.unitPriceCents, totalQuotas: t.suggestedQuotas })),
        );
        this.itemErrors.set([]);
      },
      error: (e) => {
        this.loadingItems.set(false);
        this.error.set(errorMessage(e));
      },
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
