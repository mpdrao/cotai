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
          <h1 class="cotai-page-title">Minhas listas</h1>
          @if (auth.organizerName()) {
            <p class="cotai-lead">Olá, {{ auth.organizerName() }}.</p>
          }
        </div>
        <div class="head-actions">
          <a routerLink="/lists/new" class="cotai-link-button cotai-link-button--primary">Criar minha lista</a>
          <cotai-button variant="ghost" (pressed)="logout()">Sair</cotai-button>
        </div>
      </header>

      @if (error()) {
        <p class="cotai-alert cotai-alert--error" role="alert">{{ error() }}</p>
      } @else if (lists() === null) {
        <p role="status">Carregando…</p>
      } @else if (lists()!.length === 0) {
        <cotai-card tinted>
          <h2>Nenhuma lista ainda</h2>
          <p>Escolha o tipo de evento, ajuste as sugestões de itens e compartilhe o link com a galera.</p>
          <a routerLink="/lists/new" class="cotai-link-button cotai-link-button--primary">Criar minha lista</a>
        </cotai-card>
      } @else {
        <ul class="cotai-grid cotai-grid--three lists">
          @for (list of lists(); track list.id) {
            <li>
              <a class="list-link" [routerLink]="['/lists', list.id]">
                <cotai-card>
                  <div class="card-body">
                    <cotai-status-badge [status]="badge(list.status)" />
                    <h2>{{ list.title }}</h2>
                    <p class="meta">{{ list.eventDate | date: 'longDate' }}</p>
                    <span class="open">Gerenciar lista</span>
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
    .page { display: grid; gap: 1.5rem; }
    .head { display: grid; gap: 1rem; }
    .head-actions { display: flex; flex-wrap: wrap; align-items: center; gap: 0.5rem; }
    .cotai-lead { margin: 0.25rem 0 0; }
    h2 { margin: 0; font-size: 1.125rem; overflow-wrap: anywhere; }
    cotai-card p { margin: 0.5rem 0 1rem; color: var(--cotai-muted); }
    .lists { margin: 0; padding: 0; list-style: none; }
    .list-link { display: block; height: 100%; color: inherit; text-decoration: none; border-radius: var(--cotai-radius-card); }
    .list-link cotai-card { height: 100%; }
    .card-body { display: grid; gap: 0.5rem; justify-items: start; }
    .meta { margin: 0; color: var(--cotai-muted); }
    .open { font-weight: 600; color: var(--cotai-primary); }
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
