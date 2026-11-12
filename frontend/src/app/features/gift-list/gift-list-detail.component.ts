import { Component, ElementRef, Injector, afterNextRender, computed, inject, signal, viewChild } from '@angular/core';
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
  templateUrl: './gift-list-detail.component.html',
  styleUrl: './gift-list-detail.component.scss',
})
export class GiftListDetailComponent {
  private readonly injector = inject(Injector);
  private readonly reservationForm = viewChild<ElementRef<HTMLFormElement>>('reservationForm');
  private readonly itemsList = viewChild<ElementRef<HTMLUListElement>>('itemsList');
  private readonly listNoticeEl = viewChild<ElementRef<HTMLParagraphElement>>('listNoticeEl');
  private readonly giftListService = inject(GiftListService);
  private readonly router = inject(Router);

  private readonly listSlug = location.pathname.split('/').pop() ?? '';

  protected readonly list = signal<PublicGiftList | null>(null);
  protected readonly notFound = signal(false);
  protected readonly selected = signal<PublicItem | null>(null);

  protected readonly brokenPhotos = signal<ReadonlySet<string>>(new Set());
  protected readonly quantity = signal(1);
  protected readonly submitting = signal(false);
  protected readonly error = signal('');
  protected readonly listNotice = signal('');

  protected guestName = '';
  protected guestEmail = '';

  protected readonly overall = computed(() => {
    const l = this.list();
    if (!l) return { total: 0, reserved: 0, percent: 0 };
    const total = l.items.reduce((sum, item) => sum + item.totalQuotas, 0);
    const reserved = l.items.reduce((sum, item) => sum + (item.totalQuotas - item.availableQuotas), 0);
    const percent = total > 0 ? Math.round((reserved / total) * 100) : 0;
    return { total, reserved, percent };
  });

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
    afterNextRender(
      () =>
        this.itemsList()
          ?.nativeElement.querySelector<HTMLButtonElement>(`[data-item-id="${id}"] button`)
          ?.focus(),
      { injector: this.injector },
    );
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

            this.selected.set(null);
            this.listNotice.set(errorMessage(e));

            afterNextRender(() => this.listNoticeEl()?.nativeElement.focus(), { injector: this.injector });
            this.load();
          } else {
            this.error.set(errorMessage(e));
          }
        },
      });
  }

  protected hidePhoto(url: string): void {
    this.brokenPhotos.update((set) => new Set(set).add(url));
  }

  private load(): void {
    this.giftListService.getGiftList(this.listSlug).subscribe({
      next: (l) => this.list.set(l),
      error: () => this.notFound.set(true),
    });
  }
}
