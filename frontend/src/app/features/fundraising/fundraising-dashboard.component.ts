import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { concatMap, map, range, toArray } from 'rxjs';
import { collectionProgress } from '../../core/copy/progress-copy';
import { errorMessage } from '../../core/copy/ui-copy';
import { ContributionPage, FundraisingMetrics, ItemProgress } from '../../core/models/gift-list.model';
import { OrganizerService } from '../../core/services/organizer.service';
import { CotaiButtonComponent, CotaiCardComponent, CotaiProgressComponent } from '../../shared/cotai-ui';
import { contributionsCsv } from './contributions-csv';

const PAGE_SIZE = 10;
const EXPORT_PAGE_SIZE = 50;

@Component({
  selector: 'app-fundraising-dashboard',
  imports: [CurrencyPipe, DatePipe, RouterLink, CotaiButtonComponent, CotaiCardComponent, CotaiProgressComponent],
  template: `
    <div class="page">
      <a [routerLink]="['/lists', id()]" class="cotai-back">← Voltar para a lista</a>

      @if (error()) {
        <div class="cotai-state">
          <h1>Vamos tentar de novo?</h1>
          <p class="cotai-alert cotai-alert--error" role="alert">{{ error() }}</p>
          <cotai-button variant="secondary" (pressed)="load()">Tentar novamente</cotai-button>
        </div>
      } @else if (metrics(); as m) {
        <header class="head">
          <p class="cotai-eyebrow">Arrecadação</p>
          <h1 class="cotai-page-title">{{ m.title }}</h1>
          @if (progress(); as p) {
            <div class="goal">
              <p class="goal-percent"><strong>{{ p.percent }}%</strong> da meta confirmada</p>
              <cotai-progress [value]="p.percent" label="Valor confirmado da meta" />
              <p class="goal-message">{{ p.message }}</p>
            </div>
          }
        </header>

        <dl class="kpis">
          <div>
            <dt>Total arrecadado</dt>
            <dd>{{ m.confirmedCents / 100 | currency: 'BRL' }}</dd>
            <dd class="sub">de {{ m.targetCents / 100 | currency: 'BRL' }}</dd>
          </div>
          <div>
            <dt>Cotas pagas</dt>
            <dd>{{ m.paidQuotas }}</dd>
            <dd class="sub">de {{ m.totalQuotas }}</dd>
          </div>
          <div>
            <dt>Cotas restantes</dt>
            <dd>{{ m.remainingQuotas }}</dd>
            @if (m.pendingQuotas > 0) {
              <dd class="sub">{{ m.pendingQuotas }} aguardando pagamento</dd>
            }
          </div>
          <div>
            <dt>Contribuidores</dt>
            <dd>{{ m.contributors }}</dd>
            @if (m.lastContributionAt) {
              <dd class="sub">última em {{ m.lastContributionAt | date: 'short' }}</dd>
            }
          </div>
        </dl>

        <section class="block" aria-labelledby="items-title">
          <h2 id="items-title" class="section-title">Progresso por item</h2>
          <ul class="items">
            @for (item of m.items; track item.itemId) {
              <li [class.selected]="itemFilter() === item.itemId">
                <div class="item-main">
                  <strong>{{ item.name }}</strong>
                  <span class="muted small">{{ item.unitPriceCents / 100 | currency: 'BRL' }}</span>
                </div>
                <span class="item-state">{{ itemState(item) }}</span>
                <button type="button" class="link" [attr.aria-pressed]="itemFilter() === item.itemId"
                        (click)="toggleItem(item.itemId)">
                  {{ itemFilter() === item.itemId ? 'Ver todas' : 'Ver contribuições' }}
                  <span class="cotai-visually-hidden"> de {{ item.name }}</span>
                </button>
              </li>
            }
          </ul>
        </section>

        <section class="block" aria-labelledby="feed-title">
          <div class="feed-head">
            <h2 id="feed-title" class="section-title">
              {{ filterName() ? 'Contribuições de ' + filterName() : 'Contribuições e mensagens' }}
            </h2>
            <cotai-button variant="secondary" [loading]="exporting()" [disabled]="!page() || page()!.totalElements === 0"
                          (pressed)="exportCsv()">
              Exportar CSV
            </cotai-button>
          </div>
          @if (exportError()) {
            <p class="cotai-alert cotai-alert--error" role="alert">{{ exportError() }}</p>
          }

          @if (feedError()) {
            <p class="cotai-alert cotai-alert--error" role="alert">{{ feedError() }}</p>
          } @else if (page(); as pg) {
            @if (pg.content.length === 0) {
              <cotai-card tinted>
                <p class="muted">
                  {{ filterName() ? 'Ninguém pagou este item ainda.' : 'Nenhum pagamento confirmado ainda. Compartilhe o link da lista com a galera.' }}
                </p>
              </cotai-card>
            } @else {
              <ul class="feed">
                @for (c of pg.content; track c.reservationId) {
                  <li>
                    <cotai-card>
                      <div class="row">
                        <div class="who">
                          <strong>{{ c.guestName }}</strong>
                          <span class="muted small">
                            {{ c.itemName }} · {{ c.quantity }} {{ c.quantity === 1 ? 'cota' : 'cotas' }}
                          </span>
                        </div>
                        <div class="paid">
                          <strong>{{ c.amountCents / 100 | currency: 'BRL' }}</strong>
                          <span class="muted small">{{ c.paidAt | date: 'short' }}</span>
                        </div>
                      </div>
                      @if (c.message) {
                        <blockquote class="message">{{ c.message }}</blockquote>
                      }
                    </cotai-card>
                  </li>
                }
              </ul>
              @if (pg.totalPages > 1) {
                <nav class="pager" aria-label="Paginação das contribuições">
                  <cotai-button variant="ghost" [disabled]="pg.page === 0" (pressed)="go(pg.page - 1)">Anterior</cotai-button>
                  <span aria-live="polite">Página {{ pg.page + 1 }} de {{ pg.totalPages }}</span>
                  <cotai-button variant="ghost" [disabled]="pg.page + 1 >= pg.totalPages" (pressed)="go(pg.page + 1)">
                    Próxima
                  </cotai-button>
                </nav>
              }
            }
          } @else {
            <p role="status">Buscando contribuições…</p>
          }
        </section>
      } @else {
        <p role="status">Somando as cotas…</p>
      }
    </div>
  `,
  styles: `
    .page { display: grid; gap: 2rem; max-width: 60rem; margin-inline: auto; }
    .head { display: grid; gap: .75rem; padding: 1.5rem; border-radius: var(--cotai-radius-lg); background: var(--cotai-surface-sage); }
    .goal { display: grid; gap: .5rem; }
    .goal-percent { margin: 0; color: var(--cotai-muted); }
    .goal-percent strong { margin-right: .25rem; font-size: 2rem; font-weight: 800; line-height: 1.1; color: var(--cotai-primary); font-variant-numeric: tabular-nums; }
    .goal-message { margin: 0; font-weight: 600; }
    .kpis { display: grid; grid-template-columns: minmax(0, 1fr); gap: .75rem; margin: 0; }
    .kpis div { padding: 1rem; border-left: 2px solid var(--cotai-border); min-width: 0; }
    .kpis dt { font-size: .875rem; color: var(--cotai-muted); }
    .kpis dd { margin: .5rem 0 0; font-size: 1.5rem; font-weight: 700; font-variant-numeric: tabular-nums; overflow-wrap: anywhere; }
    .kpis dd.sub { margin: .25rem 0 0; font-size: .875rem; font-weight: 400; color: var(--cotai-muted); }
    .block { display: grid; gap: 1rem; }
    .section-title { margin: 0; font-size: 1.25rem; color: var(--cotai-primary); }
    .muted { margin: 0; color: var(--cotai-muted); }
    .small { font-size: .875rem; }
    .items, .feed { display: grid; gap: .5rem; margin: 0; padding: 0; list-style: none; }
    .items li { display: grid; gap: .5rem; padding: .75rem 1rem; border: 1px solid var(--cotai-border); border-radius: var(--cotai-radius-sm); }
    .items li.selected { border-color: var(--cotai-primary); }
    .item-main { display: grid; min-width: 0; overflow-wrap: anywhere; }
    .item-state { font-weight: 600; }
    .link { justify-self: start; min-height: var(--cotai-tap-target); padding: 0; border: 0; background: none; font: inherit; font-weight: 600; color: var(--cotai-primary); text-decoration: underline; cursor: pointer; }
    .feed-head { display: grid; gap: .75rem; justify-items: start; }
    .row { display: grid; gap: .5rem; }
    .who, .paid { display: grid; min-width: 0; overflow-wrap: anywhere; }
    .message { margin: .75rem 0 0; padding-left: .75rem; border-left: 3px solid var(--cotai-border); overflow-wrap: anywhere; white-space: pre-line; }
    .pager { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: .75rem; }
    @media (min-width: 40rem) {
      .head { padding: 2rem; }
      .kpis { grid-template-columns: repeat(auto-fit, minmax(11rem, 1fr)); }
      .items li { grid-template-columns: minmax(0, 1fr) auto auto; align-items: center; gap: 1rem; }
      .feed-head { grid-template-columns: minmax(0, 1fr) auto; align-items: center; justify-items: stretch; }
      .row { grid-template-columns: minmax(0, 1fr) auto; gap: 1rem; }
      .paid { text-align: right; }
    }
  `,
})
export class FundraisingDashboardComponent implements OnInit {
  private readonly organizer = inject(OrganizerService);

  readonly id = input.required<string>();

  protected readonly metrics = signal<FundraisingMetrics | null>(null);
  protected readonly page = signal<ContributionPage | null>(null);
  protected readonly itemFilter = signal<number | null>(null);
  protected readonly error = signal('');
  protected readonly feedError = signal('');
  protected readonly exportError = signal('');
  protected readonly exporting = signal(false);

  protected readonly progress = computed(() => {
    const m = this.metrics();
    return m ? collectionProgress(m.confirmedCents, m.targetCents, m.giftListId) : null;
  });

  protected readonly filterName = computed(() => {
    const filter = this.itemFilter();
    return filter == null ? '' : (this.metrics()?.items.find((i) => i.itemId === filter)?.name ?? '');
  });

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.error.set('');
    this.metrics.set(null);
    this.organizer.metrics(this.id()).subscribe({
      next: (m) => {
        this.metrics.set(m);
        this.go(0);
      },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  protected go(pageNumber: number): void {
    this.feedError.set('');
    this.organizer.contributions(this.id(), pageNumber, PAGE_SIZE, this.itemFilter()).subscribe({
      next: (p) => this.page.set(p),
      error: (e) => this.feedError.set(errorMessage(e)),
    });
  }

  protected toggleItem(itemId: number): void {
    this.itemFilter.update((current) => (current === itemId ? null : itemId));
    this.page.set(null);
    this.go(0);
  }

  protected itemState(item: ItemProgress): string {
    if (item.paidQuotas > 0) return 'Pago';
    if (item.pendingQuotas > 0) return 'Aguardando pagamento';
    return 'Disponível';
  }

  protected exportCsv(): void {
    const first = this.page();
    if (!first) return;
    this.exporting.set(true);
    this.exportError.set('');
    const itemId = this.itemFilter();
    const pages = Math.max(1, Math.ceil(first.totalElements / EXPORT_PAGE_SIZE));
    range(0, pages)
      .pipe(
        concatMap((n) => this.organizer.contributions(this.id(), n, EXPORT_PAGE_SIZE, itemId)),
        map((p) => p.content),
        toArray(),
        map((chunks) => chunks.flat()),
      )
      .subscribe({
        next: (rows) => {
          download(contributionsCsv(rows), `arrecadacao-${this.id()}.csv`);
          this.exporting.set(false);
        },
        error: (e) => {
          this.exporting.set(false);
          this.exportError.set(errorMessage(e));
        },
      });
  }
}

function download(content: string, filename: string): void {
  const url = URL.createObjectURL(new Blob([content], { type: 'text/csv;charset=utf-8' }));
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}
