import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { errorMessage } from '../../core/copy/ui-copy';
import { GiftListStatus, GiftListSummary } from '../../core/models/organizer.model';
import { OrganizerService } from '../../core/services/organizer.service';
import { CotaiButtonComponent, CotaiCardComponent, CotaiStatus, CotaiStatusBadgeComponent } from '../../shared/cotai-ui';

@Component({
  selector: 'app-dashboard',
  imports: [DatePipe, RouterLink, CotaiButtonComponent, CotaiCardComponent, CotaiStatusBadgeComponent],
  template: `
    <div class="page">
      <header class="head">
        <div>
          @if (auth.organizerName()) {
            <p class="greeting">Olá, {{ auth.organizerName() }}.</p>
          }
          <h1 class="cotai-page-title">Minhas listas</h1>
          <p class="cotai-lead">Seus próximos encontros começam aqui.</p>
        </div>
        <div class="head-actions">
          <a routerLink="/lists/new" class="cotai-link-button cotai-link-button--primary">Criar minha lista</a>
          <cotai-button variant="ghost" (pressed)="logout()">Sair</cotai-button>
        </div>
      </header>

      @if (error()) {
        <div class="cotai-state">
          <h2>Vamos tentar de novo?</h2>
          <p class="cotai-alert cotai-alert--error" role="alert">{{ error() }}</p>
          <cotai-button variant="secondary" (pressed)="load()">Tentar novamente</cotai-button>
        </div>
      } @else if (lists() === null) {
        <div class="cotai-state" role="status">
          <h2>Buscando suas listas…</h2>
          <p>Já vamos mostrar seus encontros por aqui.</p>
        </div>
      } @else if (lists()!.length === 0) {
        <cotai-card tinted>
          <div class="cotai-state">
            <p class="cotai-eyebrow">Tudo começa com uma ideia</p>
            <h2>Qual vai ser o próximo encontro?</h2>
            <p>Escolha o tipo de evento, ajuste as sugestões de itens e compartilhe o link com a galera.</p>
            <a routerLink="/lists/new" class="cotai-link-button cotai-link-button--primary">Criar minha lista</a>
          </div>
        </cotai-card>
      } @else {
        <ul class="cotai-grid cotai-grid--three lists">
          @for (list of lists(); track list.id) {
            <li>
              <a class="list-link" [routerLink]="['/lists', list.id]">
                <cotai-card>
                  <div class="card-body">
                    <h2>{{ list.title }}</h2>
                    <p class="meta">{{ list.eventDate | date: 'longDate' }}</p>
                    <cotai-status-badge [status]="badge(list.status)" />
                    <span class="open">Gerenciar lista <span aria-hidden="true">↗</span></span>
                  </div>
                </cotai-card>
              </a>
            </li>
          }
        </ul>
      }
    </div>
  `,
  styles: `
    .page { display: grid; gap: 2.5rem; }
    .head { display: grid; gap: 1.5rem; padding-block: .5rem 2rem; border-bottom: 1px solid var(--cotai-border); }
    .greeting { margin: 0 0 .75rem; color: var(--cotai-muted); overflow-wrap: anywhere; }
    .head-actions { display: flex; flex-wrap: wrap; align-items: center; gap: 0.5rem; }
    .cotai-lead { margin: 0.25rem 0 0; }
    h2 { margin: 0; font-size: 1.375rem; line-height: 1.3; overflow-wrap: anywhere; }
    cotai-card p { margin: 0.5rem 0 1rem; color: var(--cotai-muted); }
    .lists { margin: 0; padding: 0; list-style: none; }
    .list-link { display: block; height: 100%; color: inherit; text-decoration: none; border-radius: var(--cotai-radius-card); transition: transform var(--cotai-motion), box-shadow var(--cotai-motion); }
    .list-link cotai-card { height: 100%; }
    .list-link:hover { transform: translateY(-2px); box-shadow: var(--cotai-shadow-card); --cotai-card-border: var(--cotai-input-border); }
    .card-body { display: flex; flex-direction: column; align-items: start; gap: .75rem; height: 100%; }
    .meta { margin: 0; color: var(--cotai-muted); }
    .open { display: flex; justify-content: space-between; width: 100%; margin-top: auto; padding-top: 1.5rem; font-weight: 600; color: var(--cotai-primary); }
    @media (min-width: 40rem) {
      .head { grid-template-columns: minmax(0, 1fr) auto; align-items: center; }
    }
  `,
})
export class DashboardComponent {
  protected readonly auth = inject(AuthService);
  private readonly organizer = inject(OrganizerService);
  private readonly router = inject(Router);

  protected readonly lists = signal<GiftListSummary[] | null>(null);
  protected readonly error = signal('');

  constructor() {
    this.load();
  }

  protected load(): void {
    this.error.set('');
    this.lists.set(null);
    this.organizer.myLists().subscribe({
      next: (l) => this.lists.set(l),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  protected logout(): void {
    this.auth.logout();
    this.router.navigate(['/']);
  }

  protected badge(status: GiftListStatus): CotaiStatus {
    return status.toLowerCase() as CotaiStatus;
  }
}
