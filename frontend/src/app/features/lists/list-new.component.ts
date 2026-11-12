import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Observable, Subscription, catchError, forkJoin, map, of, switchMap } from 'rxjs';
import { ORGANIZER_COPY, errorMessage } from '../../core/copy/ui-copy';
import { EventType } from '../../core/models/event-type.model';
import { GiftList } from '../../core/models/gift-list.model';
import { OrganizerService } from '../../core/services/organizer.service';
import { ItemEditorComponent } from './item-editor.component';
import { ItemPhotoComponent } from './item-photo.component';
import {
  ItemDraft,
  ItemErrors,
  draftFrom,
  draftPhotoSrc,
  draftTotalCents,
  emptyDraft,
  setDraftPhoto,
  toItemInput,
  validateDraft,
} from './item-draft';
import { CotaiButtonComponent, CotaiCardComponent } from '../../shared/cotai-ui';

const TITLE_SUGGESTIONS: Record<string, string[]> = {
  BARBECUE: ['Churrasco da firma', 'Churras de fim de ano', 'Churrasco de aniversário', 'Resenha do fim de semana'],
  BABY_SHOWER: ['Chá de bebê da Ana', 'Chá de fraldas', 'Chá revelação', 'Chegada do bebê'],
  WEDDING: ['Casamento de Ana e Pedro', 'Nosso casamento', 'Lua de mel dos noivos', 'Casa nova dos noivos'],
  OTHER: ['Aniversário de 30 anos', 'Bodas de prata', 'Formatura da turma', 'Vaquinha da galera'],
};

function photoWarning(failed: number): string {
  const what = failed === 1 ? '1 foto não foi enviada' : `${failed} fotos não foram enviadas`;
  return `Lista criada, mas ${what}. Adicione de novo no item.`;
}

function titleSuggestionsFor(type: EventType | null): string[] {
  return type ? (TITLE_SUGGESTIONS[type.code] ?? TITLE_SUGGESTIONS['OTHER']) : [];
}

@Component({
  selector: 'app-list-new',
  imports: [CurrencyPipe, FormsModule, RouterLink, ItemEditorComponent, ItemPhotoComponent, CotaiButtonComponent, CotaiCardComponent],
  templateUrl: './list-new.component.html',
  styleUrl: './list-new.component.scss',
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

  private readonly edits = signal(0);

  private templateRequest?: Subscription;

  protected title = '';
  protected eventDate = '';
  protected ttl: number | null = 30;

  protected readonly goalCents = computed(() => {
    this.edits();
    return this.items().reduce((sum, d) => sum + draftTotalCents(d), 0);
  });

  constructor() {

    inject(DestroyRef).onDestroy(() => this.releasePhotos());
    this.organizer.eventTypes().subscribe({
      next: (types) => this.eventTypes.set(types),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  protected touched(): void {
    this.edits.update((n) => n + 1);
  }

  protected chooseType(type: EventType): void {

    const previousIndex = this.titleSuggestions().indexOf(this.title);
    this.selectedType.set(type);
    if (previousIndex >= 0) this.title = this.titleSuggestions()[previousIndex] ?? this.titleSuggestions()[0];
    this.ttl = type.defaultReservationTtlMinutes;
    this.loadingItems.set(true);
    this.templateRequest?.unsubscribe();
    this.templateRequest = this.organizer.templateItems(type.code).subscribe({
      next: (templates) => {
        this.loadingItems.set(false);
        this.releasePhotos();
        this.items.set(
          templates.map((t) =>
            draftFrom({ name: t.name, unit: t.unit, unitPriceCents: t.unitPriceCents, totalQuotas: t.suggestedQuotas, imageUrl: t.imageUrl }),
          ),
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

  protected photoSrc(d: ItemDraft): string | undefined {
    this.edits();
    return draftPhotoSrc(d);
  }

  protected setPhoto(d: ItemDraft, file: File | null): void {
    setDraftPhoto(d, file);
    this.touched();
  }

  private releasePhotos(): void {
    this.items().forEach((d) => setDraftPhoto(d, null));
  }

  protected addItem(): void {
    this.items.update((list) => [...list, emptyDraft()]);
  }

  protected removeItem(index: number): void {
    setDraftPhoto(this.items()[index], null);
    this.items.update((list) => list.filter((_, i) => i !== index));
    this.itemErrors.update((list) => list.filter((_, i) => i !== index));
  }

  private uploadPhotos(list: GiftList): Observable<number> {
    const uploads = this.items().flatMap((d, i) => {
      const item = list.items[i];
      return d.photo && item
        ? [this.organizer.uploadPhoto(list.id, item.id, d.photo).pipe(map(() => 0), catchError(() => of(1)))]
        : [];
    });
    return uploads.length ? forkJoin(uploads).pipe(map((r) => r.reduce((a, b) => a + b, 0))) : of(0);
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
      .pipe(switchMap((list) => this.uploadPhotos(list).pipe(map((failed) => ({ list, failed })))))
      .subscribe({
        next: ({ list, failed }) =>
          this.router.navigate(['/lists', list.id], { state: failed ? { warning: photoWarning(failed) } : {} }),
        error: (e) => {
          this.saving.set(false);
          this.error.set(errorMessage(e, ORGANIZER_COPY));
        },
      });
  }
}
