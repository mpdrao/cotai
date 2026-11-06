import { Component, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { collectionProgress } from '../../core/copy/progress-copy';
import { ORGANIZER_COPY, errorCode, errorMessage } from '../../core/copy/ui-copy';
import { GiftList, GiftListSummaryReport, Item } from '../../core/models/gift-list.model';
import { OrganizerService } from '../../core/services/organizer.service';
import { ItemEditorComponent } from './item-editor.component';
import { ItemPhotoComponent } from './item-photo.component';
import {
  ItemDraft,
  ItemErrors,
  draftFrom,
  draftPhotoSrc,
  emptyDraft,
  setDraftPhoto,
  toItemInput,
  validateDraft,
} from './item-draft';
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
  templateUrl: './list-manage.component.html',
  styleUrl: './list-manage.component.scss',
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
  protected readonly newPhotoSrc = computed(() => {
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
  protected readonly progress = computed(() => {
    const s = this.summary();
    return s ? collectionProgress(s.confirmedCents, s.targetCents, this.id) : null;
  });
  protected readonly recentContributors = computed(() => [...(this.summary()?.contributors ?? [])].reverse().slice(0, 10));

  constructor() {
    // Show the create screen's warning once: history.state survives a reload.
    if (history.state?.warning) history.replaceState({ ...history.state, warning: undefined }, '');
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

  protected cancelNewItem(): void {
    const draft = this.newDraft();
    if (draft) setDraftPhoto(draft, null);
    this.newDraft.set(null);
  }

  protected setNewPhoto(file: File | null): void {
    const current = this.newDraft();
    if (!current) return;
    setDraftPhoto(current, file);
    this.newDraft.set({ ...current });
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
