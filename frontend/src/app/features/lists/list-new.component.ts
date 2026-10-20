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
      <header class="hero">
        <a routerLink="/dashboard" class="cotai-back">← Minhas listas</a>
        <p class="cotai-eyebrow">Nova lista</p>
        <h1 class="cotai-page-title">Criar minha lista</h1>
        <p class="cotai-lead">Escolha o tipo de evento e a gente sugere nome e itens. Dá para mudar tudo antes de publicar.</p>
      </header>

      <fieldset class="step types">
        <legend class="step-head">
          <span class="step-num" aria-hidden="true">1</span>
          <span class="step-title">Tipo de evento</span>
        </legend>
        @if (eventTypes().length === 0) {
          <p class="cotai-hint" role="status">Carregando tipos de evento…</p>
        }
        <div class="type-options">
          @for (type of eventTypes(); track type.code) {
            <label class="type" [class.type--selected]="selectedType()?.code === type.code">
              <input type="radio" name="eventType" [id]="'type-' + type.code" [value]="type.code"
                     [checked]="selectedType()?.code === type.code" (change)="chooseType(type)" />
              <span class="type-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  @switch (type.code) {
                    @case ('BARBECUE') {
                      <path d="M12 3c.8 3.2 5 5.4 5 10a5 5 0 0 1-10 0c0-2.4 1.3-4 2.4-5 .3 1.6 1.2 2.6 2.6 3-.2-3-.9-5.6 0-8Z" />
                    }
                    @case ('BABY_SHOWER') {
                      <path d="M10.5 2.5h3M10 5h4l1 3H9l1-3ZM9.5 8h5A1.5 1.5 0 0 1 16 9.5V19a2.5 2.5 0 0 1-2.5 2.5h-3A2.5 2.5 0 0 1 8 19V9.5A1.5 1.5 0 0 1 9.5 8ZM8 12.5h3M8 16h3" />
                    }
                    @case ('WEDDING') {
                      <circle cx="9" cy="14.5" r="5" /><circle cx="15" cy="14.5" r="5" /><path d="M7.5 7.5 9 5l1.5 2.5" />
                    }
                    @default {
                      <path d="M12 3v3M12 18v3M3 12h3M18 12h3M5.6 5.6l2.1 2.1M16.3 16.3l2.1 2.1M5.6 18.4l2.1-2.1M16.3 7.7l2.1-2.1" /><circle cx="12" cy="12" r="2" />
                    }
                  }
                </svg>
              </span>
              <span class="type-name">{{ type.name }}</span>
              <span class="type-check" aria-hidden="true">✓</span>
            </label>
          }
        </div>
        @if (errors()['eventType']) { <p class="cotai-field-error">{{ errors()['eventType'] }}</p> }
      </fieldset>

      <section class="step" aria-labelledby="event-details-title">
        <div class="step-head">
          <span class="step-num" aria-hidden="true">2</span>
          <h2 class="step-title" id="event-details-title">Sobre o encontro</h2>
        </div>
        <div class="details">
          <div class="cotai-field title">
            <label class="cotai-label" for="list-title">Nome da lista</label>
            <input class="cotai-input" id="list-title" name="title" maxlength="80" [placeholder]="titlePlaceholder()"
                   [(ngModel)]="title" [attr.aria-invalid]="!!errors()['title']" />
            @if (titleSuggestions().length > 0) {
              <div class="suggestions" role="group" aria-labelledby="title-suggestions-label">
                <span class="cotai-hint" id="title-suggestions-label">Ideias para {{ selectedType()!.name }}:</span>
                @for (suggestion of titleSuggestions(); track suggestion) {
                  <button type="button" class="suggestion" [attr.aria-pressed]="title === suggestion" (click)="title = suggestion">
                    {{ suggestion }}
                  </button>
                }
              </div>
            } @else {
              <span class="cotai-hint">Escolha o tipo de evento para ver ideias de nome.</span>
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

      <section class="step items" aria-labelledby="items-title">
        <div class="step-head">
          <span class="step-num" aria-hidden="true">3</span>
          <div>
            <h2 class="step-title" id="items-title">Itens</h2>
            <p class="cotai-hint">
              @if (selectedType()) {
                Sugestões para {{ selectedType()!.name }}. Mude o que quiser: nome, unidade, valor e número de cotas.
              } @else {
                Escolha o tipo de evento para receber sugestões, ou adicione os itens você mesmo.
              }
            </p>
          </div>
        </div>
        @if (loadingItems()) {
          <p class="cotai-hint" role="status">Carregando sugestões…</p>
        } @else if (items().length === 0) {
          <div class="empty">
            <p>Nenhum item ainda.</p>
            <p class="cotai-hint">Os itens viram as cotas que os convidados escolhem.</p>
          </div>
        }
        @for (item of items(); track item; let i = $index) {
          <cotai-card elevation="soft">
            <div class="item">
              <div class="item-head">
                <span class="item-index">Item {{ i + 1 }}</span>
                <span class="item-total">{{ itemTotal(item) / 100 | currency: 'BRL' }}</span>
              </div>
              <app-item-editor [draft]="item" [errors]="itemErrors()[i] ?? {}" [prefix]="'new-item-' + i" (changed)="touched()" />
              <div class="item-foot">
                <cotai-button variant="ghost" (pressed)="removeItem(i)">Remover item</cotai-button>
              </div>
            </div>
          </cotai-card>
        }
        <cotai-button variant="secondary" [fullWidth]="true" [disabled]="items().length >= 100" (pressed)="addItem()">
          + Adicionar item
        </cotai-button>
      </section>

      <p class="cotai-hint draft-note">A lista começa como rascunho. Você publica e compartilha o link quando estiver pronta.</p>

      <div class="summary">
        <div class="goal">
          <span class="goal-label">Meta da lista</span>
          <strong class="goal-value">{{ goalCents() / 100 | currency: 'BRL' }}</strong>
          <span class="goal-count">{{ items().length }} {{ items().length === 1 ? 'item' : 'itens' }}</span>
        </div>
        @if (error()) {
          <p class="cotai-alert cotai-alert--error summary-alert" role="alert">{{ error() }}</p>
        }
        <cotai-button type="submit" [fullWidth]="true" [loading]="saving()">Criar minha lista</cotai-button>
      </div>
    </form>
  `,
  styles: `
    .page { display: grid; gap: 1.5rem; max-width: 56rem; margin-inline: auto; padding-bottom: 1rem; }

    /* Header: warm gradient with soft blobs, the only decorative layer on the page. */
    .hero {
      position: relative; overflow: hidden; display: grid; gap: 0.5rem;
      padding: 1rem 1.25rem 1.75rem; border-radius: var(--cotai-radius-lg); background: var(--cotai-gradient-warm);
    }
    .hero::after {
      content: ''; position: absolute; inset: auto -3rem -4rem auto; width: 12rem; height: 12rem; border-radius: 50%;
      background: radial-gradient(circle, color-mix(in srgb, var(--cotai-terracotta) 18%, transparent), transparent 70%);
      pointer-events: none;
    }
    .hero > * { position: relative; z-index: 1; }
    .hero .cotai-back { justify-self: start; }
    .hero .cotai-eyebrow { color: var(--cotai-terracotta); text-transform: uppercase; letter-spacing: 0.08em; font-size: 0.75rem; }
    .hero .cotai-lead { margin: 0; max-width: 34rem; }

    /* Steps: white panels lifted off the cream background. */
    .step {
      display: grid; gap: 1.25rem; min-width: 0; margin: 0; padding: 1.25rem;
      border: 1px solid var(--cotai-border); border-radius: var(--cotai-radius-card);
      background: var(--cotai-surface); box-shadow: var(--cotai-shadow-card);
    }
    .step-head { display: flex; align-items: flex-start; gap: 0.75rem; padding: 0; }
    legend.step-head { float: left; width: 100%; margin-bottom: 1.25rem; }
    legend.step-head + * { clear: both; }
    .step-num {
      flex: none; display: grid; place-items: center; width: 2rem; height: 2rem; border-radius: 50%;
      background: var(--cotai-primary); color: var(--cotai-surface); font-size: 0.875rem; font-weight: 700;
      box-shadow: 0 0 0 4px var(--cotai-success-bg);
    }
    .step-title { margin: 0; font-size: 1.125rem; font-weight: 700; line-height: 2rem; color: var(--cotai-ink); }
    .step-head .cotai-hint { margin-top: 0.125rem; }

    /* Event type tiles */
    .type-options { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0.75rem; }
    .type {
      position: relative; display: grid; justify-items: center; align-content: center; gap: 0.5rem; min-height: 7rem;
      padding: 1rem 0.5rem; border: 1px solid var(--cotai-border); border-radius: var(--cotai-radius-card);
      background: var(--cotai-surface); font-weight: 600; text-align: center; cursor: pointer;
      transition: border-color var(--cotai-motion), box-shadow var(--cotai-motion), transform var(--cotai-motion), background var(--cotai-motion);
    }
    .type:hover { border-color: var(--cotai-secondary); box-shadow: var(--cotai-shadow-hover); transform: translateY(-2px); }
    .type input { position: absolute; opacity: 0; inset: 0; margin: 0; cursor: pointer; }
    .type:has(input:focus-visible) { outline: 3px solid var(--cotai-primary); outline-offset: 3px; }
    .type-icon {
      display: grid; place-items: center; width: 3rem; height: 3rem; border-radius: 50%;
      background: var(--cotai-surface-peach); color: var(--cotai-terracotta);
      transition: background var(--cotai-motion), color var(--cotai-motion);
    }
    .type-icon svg { width: 1.5rem; height: 1.5rem; }
    .type-check {
      position: absolute; top: 0.5rem; right: 0.5rem; display: grid; place-items: center; width: 1.5rem; height: 1.5rem;
      border-radius: 50%; background: var(--cotai-primary); color: var(--cotai-surface); font-size: 0.75rem;
      opacity: 0; transform: scale(0.6); transition: opacity var(--cotai-motion), transform var(--cotai-motion);
    }
    .type--selected {
      border-color: var(--cotai-primary); background: var(--cotai-surface-sage); color: var(--cotai-primary);
      box-shadow: inset 0 0 0 1px var(--cotai-primary), var(--cotai-shadow-card);
    }
    .type--selected .type-icon { background: var(--cotai-primary); color: var(--cotai-surface); }
    .type--selected .type-check { opacity: 1; transform: scale(1); }

    /* Details */
    .details { display: grid; gap: 1.5rem; }
    .suggestions { display: flex; flex-wrap: wrap; align-items: center; gap: 0.5rem; }
    .suggestions .cotai-hint { flex-basis: 100%; }
    .suggestion {
      padding: 0.5rem 0.9rem; border: 1px dashed var(--cotai-secondary); border-radius: var(--cotai-radius-pill);
      background: var(--cotai-surface); color: var(--cotai-primary); font-size: 0.875rem; font-weight: 500; cursor: pointer;
      transition: background var(--cotai-motion), border-color var(--cotai-motion);
    }
    .suggestion:hover { background: var(--cotai-surface-sage); border-style: solid; }
    .suggestion[aria-pressed='true'] { background: var(--cotai-primary); border: 1px solid var(--cotai-primary); color: var(--cotai-surface); }

    /* Items */
    .item { display: grid; gap: 1rem; }
    .item-head { display: flex; align-items: center; justify-content: space-between; gap: 0.5rem; }
    .item-index {
      padding: 0.25rem 0.75rem; border-radius: var(--cotai-radius-pill); background: var(--cotai-surface-sage);
      color: var(--cotai-primary); font-size: 0.75rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.06em;
    }
    .item-total { font-weight: 700; color: var(--cotai-ink); }
    .item-foot { display: flex; justify-content: flex-end; margin-top: -0.25rem; }
    .empty {
      display: grid; gap: 0.25rem; padding: 1.5rem; text-align: center; border: 1px dashed var(--cotai-input-border);
      border-radius: var(--cotai-radius-card); background: var(--cotai-background);
    }
    .empty p { margin: 0; }

    /* Sticky summary: always shows the goal next to the main action. */
    .draft-note { text-align: center; }
    .summary {
      position: sticky; bottom: 0.75rem; z-index: 2; display: grid; gap: 0.75rem; padding: 1rem;
      border: 1px solid var(--cotai-border); border-radius: var(--cotai-radius-lg);
      background: var(--cotai-surface-glass); backdrop-filter: blur(12px); box-shadow: var(--cotai-shadow-raised);
    }
    .goal { display: grid; grid-template-columns: 1fr auto; align-items: baseline; column-gap: 0.75rem; }
    .goal-label { grid-column: 1 / -1; font-size: 0.75rem; font-weight: 600; color: var(--cotai-muted); text-transform: uppercase; letter-spacing: 0.06em; }
    .goal-value { font-size: 1.5rem; font-weight: 800; line-height: 1.2; color: var(--cotai-primary); }
    .goal-count { font-size: 0.875rem; color: var(--cotai-muted); }
    .summary-alert { font-size: 0.875rem; }

    @media (min-width: 40rem) {
      .page { gap: 2rem; }
      .hero { padding: 1.25rem 2rem 2.25rem; }
      .step { padding: 2rem; }
      .type-options { grid-template-columns: repeat(4, minmax(0, 1fr)); }
      .details { grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); }
      .title { grid-column: 1 / -1; }
      .summary { grid-template-columns: minmax(0, 1fr) auto; align-items: center; padding: 1rem 1rem 1rem 1.5rem; }
      .goal { grid-template-columns: auto auto; justify-content: start; }
      .summary-alert { grid-column: 1 / -1; grid-row: 1; }
      .summary cotai-button { width: auto; min-width: 14rem; }
    }
    @media (prefers-reduced-motion: reduce) {
      .type:hover { transform: none; }
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
    this.organizer.templateItems(type.code).subscribe({
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
