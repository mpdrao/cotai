import { Component, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { ORGANIZER_COPY, errorCode, errorMessage } from '../../core/copy/ui-copy';
import { GiftList, GiftListSummaryReport, Item } from '../../core/models/organizer.model';
import { OrganizerService } from '../../core/services/organizer.service';
import { ItemEditorComponent } from './item-editor.component';
import { ItemPhotoComponent } from './item-photo.component';
import { ItemDraft, ItemErrors, draftFrom, draftPhotoSrc, emptyDraft, setDraftPhoto, toItemInput, validateDraft } from './item-draft';
import {
  CotaiButtonComponent,
  CotaiCardComponent,
  CotaiProgressComponent,
  CotaiStatus,
  CotaiStatusBadgeComponent,
} from '../../shared/cotai-ui';

/** UC04-UC07: publish, share, edit items, follow the collection and close the list. */
@Component({
  selector: 'app-list-manage',
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    RouterLink,
    ItemEditorComponent,
    ItemPhotoComponent,
    CotaiButtonComponent,
    CotaiCardComponent,
    CotaiProgressComponent,
    CotaiStatusBadgeComponent,
  ],
  template: `
    <div class="page">
      <a routerLink="/dashboard" class="cotai-back">← Minhas listas</a>

      @if (list(); as l) {
        <header class="head">
          <cotai-status-badge [status]="badge()" />
          <h1 class="cotai-page-title">{{ l.title }}</h1>
          <p class="meta">{{ l.eventDate | date: 'longDate' }} · prazo para pagar: {{ l.reservationTtlMinutes }} min</p>
        </header>

        @if (warning()) {
          <p class="cotai-alert cotai-alert--warning" role="status">{{ warning() }}</p>
        }
        @if (notice()) {
          <p class="cotai-alert cotai-alert--success" role="status">{{ notice() }}</p>
        }

        <!-- Status and the action that moves the list forward -->
        <cotai-card [tinted]="l.status !== 'PUBLISHED'">
          @switch (l.status) {
            @case ('DRAFT') {
              <h2>Rascunho</h2>
              <p class="muted">Só você vê esta lista. Publique quando os itens estiverem prontos para receber o link de compartilhamento.</p>
              @if (actionError()) {
                <p class="cotai-alert cotai-alert--error" role="alert">{{ actionError() }}</p>
              }
              <cotai-button [fullWidth]="true" [loading]="busy()" (pressed)="publish()">Publicar lista</cotai-button>
            }
            @case ('PUBLISHED') {
              <h2>Compartilhar lista</h2>
              <p class="muted">Quem abrir o link escolhe as cotas e paga, sem precisar de cadastro.</p>
              <p class="share-url">{{ publicUrl() }}</p>
              <div class="actions">
                <cotai-button [fullWidth]="true" (pressed)="copyLink()">Copiar link</cotai-button>
                <a class="cotai-link-button" [routerLink]="['/l', l.slug]" target="_blank" rel="noopener">Ver página da lista</a>
              </div>
              @if (copied()) {
                <p class="muted small" role="status">Link copiado.</p>
              }
              <div class="close-area">
                @if (!confirmClose()) {
                  <cotai-button variant="ghost" [fullWidth]="true" (pressed)="confirmClose.set(true)">Encerrar lista</cotai-button>
                } @else {
                  <p class="cotai-alert cotai-alert--warning">
                    Ninguém mais vai conseguir reservar. Quem já reservou ainda pode pagar até o fim do prazo.
                  </p>
                  @if (actionError()) {
                    <p class="cotai-alert cotai-alert--error" role="alert">{{ actionError() }}</p>
                  }
                  <div class="actions">
                    <cotai-button [fullWidth]="true" [loading]="busy()" (pressed)="close()">Sim, encerrar lista</cotai-button>
                    <cotai-button variant="secondary" [fullWidth]="true" (pressed)="confirmClose.set(false)">Voltar</cotai-button>
                  </div>
                }
              </div>
            }
            @case ('CLOSED') {
              <h2>Lista encerrada</h2>
              <p class="muted">A lista não recebe novas reservas. As reservas que ainda estavam no prazo podem ser pagas.</p>
              <a class="cotai-link-button" [routerLink]="['/l', l.slug]" target="_blank" rel="noopener">Ver página da lista</a>
            }
          }
        </cotai-card>

        <!-- Collection summary (UC06) -->
        @if (summary(); as s) {
          <section class="block" aria-labelledby="summary-title">
            <h2 id="summary-title" class="section-title">Arrecadação</h2>
            <cotai-progress [value]="s.targetCents ? (s.confirmedCents / s.targetCents) * 100 : 0" label="Valor confirmado da meta" />
            <dl class="tiles">
              <div><dt>Confirmado</dt><dd>{{ s.confirmedCents / 100 | currency: 'BRL' }}</dd></div>
              <div><dt>Aguardando pagamento</dt><dd>{{ s.pendingCents / 100 | currency: 'BRL' }}</dd></div>
              <div><dt>Meta</dt><dd>{{ s.targetCents / 100 | currency: 'BRL' }}</dd></div>
              @if (s.refundRequiredCents > 0) {
                <div class="refund"><dt>Pagamentos fora do prazo</dt><dd>{{ s.refundRequiredCents / 100 | currency: 'BRL' }}</dd></div>
              }
            </dl>
            @if (s.refundRequiredCents > 0) {
              <p class="cotai-alert cotai-alert--warning">
                Chegaram pagamentos depois do prazo da reserva. Esse valor precisa ser estornado; as reservas não foram reabertas.
              </p>
            }

            <cotai-card>
              <h3>Últimas contribuições</h3>
              @if (s.contributors.length === 0) {
                <p class="muted">Nenhum pagamento confirmado ainda.</p>
              } @else {
                <ul class="contributors">
                  @for (c of recentContributors(); track $index) {
                    <li>
                      <div class="who">
                        <strong>{{ c.guestName }}</strong>
                        <span class="muted small">{{ c.itemName }} · {{ c.quantity }} {{ c.quantity === 1 ? 'cota' : 'cotas' }}</span>
                      </div>
                      <div class="paid">
                        <strong>{{ c.amountCents / 100 | currency: 'BRL' }}</strong>
                        <span class="muted small">{{ c.confirmedAt | date: 'short' }}</span>
                      </div>
                    </li>
                  }
                </ul>
              }
            </cotai-card>
          </section>
        }

        <!-- Items (UC05) -->
        <section class="block" aria-labelledby="items-title">
          <h2 id="items-title" class="section-title">Itens</h2>
          @if (activeItems().length === 0) {
            <p class="cotai-alert cotai-alert--warning">A lista ainda não tem itens. Adicione pelo menos um para publicar.</p>
          }
          @for (item of activeItems(); track item.id) {
            <cotai-card>
              @if (editingId() === item.id) {
                <div class="item-edit">
                  <app-item-editor [draft]="editDraft()!" [errors]="editErrors()" [prefix]="'item-' + item.id" />
                  @if (itemError()) {
                    <p class="cotai-alert cotai-alert--error" role="alert">{{ itemError() }}</p>
                  }
                  <p class="cotai-hint">Mudar o valor não afeta reservas já feitas: elas mantêm o valor da hora da reserva.</p>
                  <div class="actions">
                    <cotai-button [fullWidth]="true" [loading]="busy()" (pressed)="saveItem(item)">Salvar item</cotai-button>
                    <cotai-button variant="secondary" [fullWidth]="true" (pressed)="cancelEdit()">Cancelar</cotai-button>
                  </div>
                </div>
              } @else {
                <div class="item">
                  <div class="item-info">
                    <app-item-photo [src]="item.imageUrl" [editable]="l.status !== 'CLOSED'" [busy]="photoBusyId() === item.id"
                                    [error]="photoErrorId() === item.id ? photoError() : ''"
                                    (picked)="uploadPhoto(item, $event)" (removed)="removePhoto(item)" />
                    <h3>{{ item.name }}</h3>
                    <p class="muted">{{ item.unit }} · {{ item.unitPriceCents / 100 | currency: 'BRL' }} por cota · {{ item.totalQuotas }} cotas</p>
                    @if (totalsFor(item.id); as t) {
                      <cotai-progress [value]="((t.confirmedQuotas + t.pendingQuotas) / t.totalQuotas) * 100"
                                      [label]="'Cotas reservadas de ' + item.name" />
                      <p class="small">
                        {{ t.confirmedQuotas }} pagas · {{ t.pendingQuotas }} aguardando · {{ t.availableQuotas }} livres
                      </p>
                    }
                  </div>
                  @if (l.status !== 'CLOSED') {
                    @if (confirmRemoveId() === item.id) {
                      <div class="confirm">
                        <p class="small">Remover "{{ item.name }}" da lista?</p>
                        @if (itemError()) {
                          <p class="cotai-alert cotai-alert--error" role="alert">{{ itemError() }}</p>
                        }
                        <div class="actions">
                          <cotai-button [fullWidth]="true" [loading]="busy()" (pressed)="removeItem(item)">Sim, remover</cotai-button>
                          <cotai-button variant="secondary" [fullWidth]="true" (pressed)="confirmRemoveId.set(null)">Voltar</cotai-button>
                        </div>
                      </div>
                    } @else {
                      <div class="item-actions">
                        <cotai-button variant="secondary" (pressed)="startEdit(item)">Editar</cotai-button>
                        <cotai-button variant="ghost" (pressed)="askRemove(item)">Remover</cotai-button>
                      </div>
                    }
                  }
                </div>
              }
            </cotai-card>
          }

          @if (l.status !== 'CLOSED') {
            @if (newDraft(); as d) {
              <cotai-card tinted>
                <div class="item-edit">
                  <h3>Novo item</h3>
                  <app-item-photo [src]="newPhotoSrc()" (picked)="setNewPhoto($event)" (removed)="setNewPhoto(null)" />
                  <app-item-editor [draft]="d" [errors]="newErrors()" prefix="item-new" />
                  @if (newError()) {
                    <p class="cotai-alert cotai-alert--error" role="alert">{{ newError() }}</p>
                  }
                  <div class="actions">
                    <cotai-button [fullWidth]="true" [loading]="busy()" (pressed)="saveNewItem()">Adicionar item</cotai-button>
                    <cotai-button variant="secondary" [fullWidth]="true" (pressed)="newDraft.set(null)">Cancelar</cotai-button>
                  </div>
                </div>
              </cotai-card>
            } @else {
              <cotai-button variant="secondary" [fullWidth]="true" (pressed)="openNewItem()">+ Adicionar item</cotai-button>
            }
          }
        </section>

        <!-- List details -->
        @if (l.status !== 'CLOSED') {
          <section class="block" aria-labelledby="details-title">
            <h2 id="details-title" class="section-title">Dados da lista</h2>
            <cotai-card>
              <form class="details" (ngSubmit)="saveDetails()" novalidate>
                <label class="cotai-field wide">
                  <span class="cotai-label">Nome da lista</span>
                  <input class="cotai-input" id="edit-title" name="title" maxlength="80" [(ngModel)]="title"
                         [attr.aria-invalid]="!!detailErrors()['title']" />
                  @if (detailErrors()['title']) { <span class="cotai-field-error">{{ detailErrors()['title'] }}</span> }
                </label>
                <label class="cotai-field">
                  <span class="cotai-label">Data do evento</span>
                  <input class="cotai-input" id="edit-date" name="eventDate" type="date" [(ngModel)]="eventDate"
                         [attr.aria-invalid]="!!detailErrors()['eventDate']" />
                  @if (detailErrors()['eventDate']) { <span class="cotai-field-error">{{ detailErrors()['eventDate'] }}</span> }
                </label>
                <label class="cotai-field">
                  <span class="cotai-label">Prazo para pagar (minutos)</span>
                  <input class="cotai-input" id="edit-ttl" name="ttl" type="number" inputmode="numeric" min="10" max="1440"
                         [(ngModel)]="ttl" aria-describedby="edit-ttl-hint" [attr.aria-invalid]="!!detailErrors()['ttl']" />
                  <span class="cotai-hint" id="edit-ttl-hint">Vale para as próximas reservas.</span>
                  @if (detailErrors()['ttl']) { <span class="cotai-field-error">{{ detailErrors()['ttl'] }}</span> }
                </label>
                @if (detailsError()) {
                  <p class="cotai-alert cotai-alert--error wide" role="alert">{{ detailsError() }}</p>
                }
                <div class="wide">
                  <cotai-button type="submit" variant="secondary" [loading]="busy()">Salvar dados</cotai-button>
                </div>
              </form>
            </cotai-card>
          </section>
        }
      } @else if (loadError()) {
        <cotai-card tinted>
          <h1 class="state-title">{{ loadError() }}</h1>
          <a routerLink="/dashboard">Voltar para minhas listas</a>
        </cotai-card>
      } @else {
        <p role="status">Carregando…</p>
      }
    </div>
  `,
  styles: `
    .page { display: grid; gap: 2rem; max-width: 60rem; margin-inline: auto; }
    .head { display: grid; gap: .75rem; justify-items: start; padding: 1.5rem; border-radius: var(--cotai-radius-lg); background: var(--cotai-surface-sage); }
    .meta, .muted { margin: 0; color: var(--cotai-muted); }
    .small { margin: 0; font-size: 0.875rem; }
    h2 { margin: 0 0 0.5rem; font-size: 1.125rem; }
    h3 { margin: 0; font-size: 1.0625rem; font-weight: 600; overflow-wrap: anywhere; }
    cotai-card .muted { margin-bottom: 1rem; }
    .section-title { margin: 0; font-size: 1.25rem; color: var(--cotai-primary); }
    .block { display: grid; gap: 1.5rem; margin-top: 1rem; }
    .share-url {
      margin: 0 0 1rem; padding: 0.75rem 1rem; border-radius: var(--cotai-radius-sm);
      background: var(--cotai-background); font-size: 0.875rem; overflow-wrap: anywhere;
    }
    .actions { display: grid; gap: 0.5rem; }
    .close-area { display: grid; gap: 0.75rem; margin-top: 1rem; padding-top: 1rem; border-top: 1px solid var(--cotai-border); }
    .tiles { display: grid; grid-template-columns: minmax(0, 1fr); gap: 0.75rem; margin: 0; }
    .tiles div {
      padding: 1rem; border-left: 2px solid var(--cotai-border); min-width: 0;
    }
    .tiles dt { font-size: 0.875rem; color: var(--cotai-muted); }
    .tiles dd { margin: .5rem 0 0; font-size: 1.5rem; font-weight: 700; font-variant-numeric: tabular-nums; overflow-wrap: anywhere; }
    .tiles .refund { background: var(--cotai-warning-bg); }
    .contributors { display: grid; gap: 0.75rem; margin: 0.75rem 0 0; padding: 0; list-style: none; }
    .contributors li { display: grid; gap: .5rem; padding-block: .75rem; border-bottom: 1px solid var(--cotai-border); }
    .who, .paid { display: grid; min-width: 0; }
    .paid { text-align: left; overflow-wrap: anywhere; }
    .item { display: grid; gap: 1rem; }
    .item-info { display: grid; gap: 0.375rem; min-width: 0; }
    .item-info app-item-photo { margin-bottom: 0.5rem; }
    .item-actions { display: flex; flex-wrap: wrap; gap: 0.5rem; }
    .item-edit, .confirm { display: grid; gap: 0.75rem; }
    .details { display: grid; gap: 1.5rem; }
    .state-title { font-size: 1.25rem; }
    @media (min-width: 40rem) {
      .head { padding: 2rem; }
      .contributors li { grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 1rem; }
      .paid { text-align: right; }
      .actions { grid-template-columns: repeat(2, minmax(0, 1fr)); }
      .tiles { grid-template-columns: repeat(auto-fit, minmax(10rem, 1fr)); }
      .details { grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); }
      .wide { grid-column: 1 / -1; }
    }
    @media (min-width: 48rem) {
      .item { grid-template-columns: minmax(0, 1fr) auto; align-items: center; }
    }
  `,
})
export class ListManageComponent {
  private readonly organizer = inject(OrganizerService);
  private readonly id = inject(ActivatedRoute).snapshot.paramMap.get('id') ?? '';

  protected readonly list = signal<GiftList | null>(null);
  protected readonly summary = signal<GiftListSummaryReport | null>(null);
  protected readonly loadError = signal('');
  protected readonly notice = signal('');
  protected readonly actionError = signal('');
  protected readonly busy = signal(false);
  protected readonly copied = signal(false);
  protected readonly confirmClose = signal(false);

  protected readonly editingId = signal<number | null>(null);
  protected readonly editDraft = signal<ItemDraft | null>(null);
  protected readonly editErrors = signal<ItemErrors>({});
  protected readonly itemError = signal('');
  protected readonly confirmRemoveId = signal<number | null>(null);
  protected readonly newDraft = signal<ItemDraft | null>(null);
  protected readonly newErrors = signal<ItemErrors>({});
  protected readonly newError = signal('');
  /** Bumped when the new item's photo changes (the draft is mutated in place). */
  private readonly newPhotoEdits = signal(0);
  protected readonly newPhotoSrc = computed(() => {
    this.newPhotoEdits();
    const d = this.newDraft();
    return d ? draftPhotoSrc(d) : undefined;
  });
  protected readonly photoBusyId = signal<number | null>(null);
  protected readonly photoErrorId = signal<number | null>(null);
  protected readonly photoError = signal('');
  /** Set by the create screen when some photos failed to upload. */
  protected readonly warning = signal<string>(history.state?.warning ?? '');

  protected readonly detailErrors = signal<Record<string, string>>({});
  protected readonly detailsError = signal('');
  protected title = '';
  protected eventDate = '';
  protected ttl: number | null = null;

  protected readonly badge = computed(() => (this.list()?.status.toLowerCase() ?? 'draft') as CotaiStatus);
  protected readonly activeItems = computed(() => this.list()?.items.filter((i) => !i.archived) ?? []);
  protected readonly publicUrl = computed(() => `${location.origin}/l/${this.list()?.slug ?? ''}`);
  protected readonly recentContributors = computed(() => [...(this.summary()?.contributors ?? [])].reverse().slice(0, 10));

  constructor() {
    this.reload();
  }

  protected totalsFor(itemId: number) {
    return this.summary()?.items.find((t) => t.itemId === itemId) ?? null;
  }

  protected publish(): void {
    this.run(this.organizer.publish(this.id), 'Lista publicada. Agora é só compartilhar o link.');
  }

  protected close(): void {
    this.run(this.organizer.close(this.id), 'Lista encerrada.');
  }

  protected copyLink(): void {
    navigator.clipboard?.writeText(this.publicUrl()).then(
      () => this.copied.set(true),
      () => this.copied.set(false),
    );
  }

  protected startEdit(item: Item): void {
    this.editingId.set(item.id);
    this.editDraft.set(draftFrom(item));
    this.editErrors.set({});
    this.itemError.set('');
    this.confirmRemoveId.set(null);
  }

  protected cancelEdit(): void {
    this.editingId.set(null);
    this.editDraft.set(null);
  }

  protected saveItem(item: Item): void {
    const draft = this.editDraft()!;
    const errors = validateDraft(draft);
    this.editErrors.set(errors);
    if (Object.keys(errors).length > 0) return;
    this.busy.set(true);
    this.organizer.updateItem(this.id, item.id, toItemInput(draft)).subscribe({
      next: () => {
        this.cancelEdit();
        this.afterChange('Item salvo.');
      },
      error: (e) => {
        this.busy.set(false);
        this.itemError.set(this.itemMessage(e));
      },
    });
  }

  protected askRemove(item: Item): void {
    this.confirmRemoveId.set(item.id);
    this.itemError.set('');
    this.cancelEdit();
  }

  protected removeItem(item: Item): void {
    this.busy.set(true);
    this.organizer.archiveItem(this.id, item.id).subscribe({
      next: () => {
        this.confirmRemoveId.set(null);
        this.afterChange('Item removido.');
      },
      error: (e) => {
        this.busy.set(false);
        this.itemError.set(this.itemMessage(e));
      },
    });
  }

  protected openNewItem(): void {
    this.newDraft.set(emptyDraft());
    this.newErrors.set({});
    this.newError.set('');
  }

  protected uploadPhoto(item: Item, file: File): void {
    this.photoAction(item, this.organizer.uploadPhoto(this.id, item.id, file), 'Foto salva.');
  }

  protected removePhoto(item: Item): void {
    this.photoAction(item, this.organizer.removePhoto(this.id, item.id), 'Foto removida.');
  }

  private photoAction(item: Item, action: ReturnType<OrganizerService['uploadPhoto']>, success: string): void {
    this.photoBusyId.set(item.id);
    this.photoErrorId.set(null);
    action.subscribe({
      next: () => {
        this.photoBusyId.set(null);
        this.warning.set('');
        this.afterChange(success);
      },
      error: (e) => {
        this.photoBusyId.set(null);
        this.photoErrorId.set(item.id);
        this.photoError.set(errorMessage(e, ORGANIZER_COPY));
      },
    });
  }

  protected setNewPhoto(file: File | null): void {
    setDraftPhoto(this.newDraft()!, file);
    this.newPhotoEdits.update((n) => n + 1);
  }

  protected saveNewItem(): void {
    const draft = this.newDraft()!;
    const errors = validateDraft(draft);
    this.newErrors.set(errors);
    if (Object.keys(errors).length > 0) return;
    this.busy.set(true);
    this.organizer.addItem(this.id, toItemInput(draft)).subscribe({
      next: (item) => {
        this.newDraft.set(null);
        if (!draft.photo) return this.afterChange('Item adicionado.');
        this.organizer.uploadPhoto(this.id, item.id, draft.photo).subscribe({
          next: () => this.afterChange('Item adicionado.'),
          error: (e) => {
            this.warning.set(`Item adicionado, mas a foto não foi enviada: ${errorMessage(e, ORGANIZER_COPY)}`);
            this.afterChange('');
          },
        });
        setDraftPhoto(draft, null);
      },
      error: (e) => {
        this.busy.set(false);
        this.newError.set(errorMessage(e, ORGANIZER_COPY));
      },
    });
  }

  protected saveDetails(): void {
    const errors: Record<string, string> = {};
    if (this.title.trim().length < 3) errors['title'] = 'Use pelo menos 3 caracteres.';
    if (!this.eventDate) errors['eventDate'] = 'Informe a data do evento.';
    if (this.ttl === null || this.ttl < 10 || this.ttl > 1440) errors['ttl'] = 'Use de 10 a 1440 minutos.';
    this.detailErrors.set(errors);
    this.detailsError.set('');
    if (Object.keys(errors).length > 0) return;
    this.busy.set(true);
    this.organizer
      .update(this.id, { title: this.title.trim(), eventDate: this.eventDate, reservationTtlMinutes: this.ttl! })
      .subscribe({
        next: (l) => {
          this.setList(l);
          this.busy.set(false);
          this.notice.set('Dados da lista salvos.');
        },
        error: (e) => {
          this.busy.set(false);
          this.detailsError.set(errorMessage(e, ORGANIZER_COPY));
        },
      });
  }

  // ----------------------------------------------------------------------------------------

  private run(action: ReturnType<OrganizerService['publish']>, success: string): void {
    this.busy.set(true);
    this.actionError.set('');
    action.subscribe({
      next: (l) => {
        this.setList(l);
        this.busy.set(false);
        this.confirmClose.set(false);
        this.notice.set(success);
        this.loadSummary();
      },
      error: (e) => {
        this.busy.set(false);
        this.actionError.set(errorMessage(e, ORGANIZER_COPY));
      },
    });
  }

  private afterChange(message: string): void {
    this.busy.set(false);
    this.notice.set(message);
    this.reload();
  }

  private itemMessage(e: unknown): string {
    if (errorCode(e) === 'QUOTAS_BELOW_OCCUPIED' && e instanceof HttpErrorResponse && e.error?.occupiedQuotas != null) {
      return `Esse item já tem ${e.error.occupiedQuotas} cotas reservadas. O total não pode ser menor que isso.`;
    }
    return errorMessage(e, ORGANIZER_COPY);
  }

  private reload(): void {
    this.organizer.get(this.id).subscribe({
      next: (l) => this.setList(l),
      error: (e) =>
        this.loadError.set(errorCode(e) === 'NOT_FOUND' ? 'Lista não encontrada' : errorMessage(e, ORGANIZER_COPY)),
    });
    this.loadSummary();
  }

  private loadSummary(): void {
    this.organizer.summary(this.id).subscribe({ next: (s) => this.summary.set(s), error: () => this.summary.set(null) });
  }

  private setList(l: GiftList): void {
    this.list.set(l);
    this.title = l.title;
    this.eventDate = l.eventDate;
    this.ttl = l.reservationTtlMinutes;
  }
}
