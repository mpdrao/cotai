
import { Component, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';

interface ProxyStatus {
  enabled: boolean;
  reachable: boolean;
  host: string;
  port: number;
  username: string;
  authenticationRequired: boolean;
  scope: string;
  message: string;
}

interface ProxyTestResult {
  success: boolean;
  message: string;
}

@Component({
  selector: 'app-proxy',
  imports: [RouterLink],
  template: `
    <main class="page">
      <a routerLink="/dashboard" class="back">← Voltar ao painel</a>

      <header class="heading">
        <p class="eyebrow">INFRAESTRUTURA DO COTAI</p>
        <h1 class="cotai-page-title">Gerenciamento de proxy</h1>
        <p class="lead">
          Acompanhe o servidor que encaminha as conexões externas
          iniciadas pelo backend.
        </p>
      </header>

      @if (loading()) {
        <section class="panel" role="status">
          Consultando o servidor proxy…
        </section>
      } @else if (status(); as proxy) {
        <section class="panel">
          <div class="status-line">
            <div>
              <p class="eyebrow">STATUS DO SERVIDOR</p>
              <h2>
                {{
                  !proxy.enabled
                    ? 'Proxy desativado'
                    : proxy.reachable
                      ? 'Servidor proxy acessível'
                      : 'Proxy indisponível'
                }}
              </h2>
            </div>

            <span
              class="pill"
              [class.online]="proxy.enabled && proxy.reachable"
            >
              {{
                !proxy.enabled
                  ? 'Desativado'
                  : proxy.reachable
                    ? 'Acessível'
                    : 'Inativo'
              }}
            </span>
          </div>

          <p class="message" [class.error]="proxy.enabled && !proxy.reachable">
            {{ proxy.message }}
          </p>

          <div class="details">
            <div class="detail">
              <span>Endereço interno</span>
              <strong>{{ proxy.host }}:{{ proxy.port }}</strong>
            </div>

            <div class="detail">
              <span>Usuário de autenticação</span>
              <strong>{{ proxy.username }}</strong>
            </div>

            <div class="detail">
              <span>Senha</span>
              <strong>Configurada no ambiente · não exibida</strong>
            </div>

            <div class="detail">
              <span>Autenticação</span>
              <strong>
                {{
                  proxy.authenticationRequired
                    ? 'Obrigatória'
                    : 'Desativada'
                }}
              </strong>
            </div>
          </div>

          <div class="scope">
            <strong>Escopo de utilização</strong>
            <p>
              {{ proxy.scope }}. O tráfego normal do navegador e os
              demais programas do computador não são redirecionados.
            </p>
          </div>

          <div class="actions">
            <button
              type="button"
              class="refresh"
              [disabled]="loading()"
              (click)="refresh()"
            >
              Atualizar status
            </button>

            <button
              type="button"
              class="refresh test-button"
              [disabled]="testing() || !proxy.enabled"
              (click)="testConnection()"
            >
              {{
                testing()
                  ? 'Testando conexão…'
                  : 'Testar conexão'
              }}
            </button>
          </div>

          @if (testResult(); as result) {
            <div
              class="test-result"
              [class.test-success]="result.success"
              [class.test-error]="!result.success"
              role="status"
            >
              <strong>
                {{
                  result.success
                    ? 'Teste realizado com sucesso'
                    : 'Falha no teste de conexão'
                }}
              </strong>

              <p>{{ result.message }}</p>
            </div>
          }

          <p class="test-note">
            O teste solicita ao backend uma conexão HTTPS externa.
            O resultado permite verificar a comunicação e a
            autenticação do proxy quando ele está habilitado.
          </p>
        </section>
      } @else {
        <section class="panel error" role="alert">
          <h2>Não foi possível consultar o proxy</h2>
          <p>{{ error() }}</p>

          <button
            type="button"
            class="refresh"
            (click)="refresh()"
          >
            Tentar novamente
          </button>
        </section>
      }

      <p class="footnote">
        As credenciais são mantidas nas variáveis de ambiente do
        backend e do Docker. Por segurança, a senha não é retornada
        pela API nem exibida nesta página.
      </p>
    </main>
  `,
  styles: `
    :host { display: block; }

    .page {
      width: min(100% - 2rem, 54rem);
      margin: 2.5rem auto;
      display: grid;
      gap: 1.5rem;
    }

    .back {
      color: var(--cotai-primary);
      text-decoration: none;
      font-weight: 600;
    }

    .heading {
      display: grid;
      gap: .45rem;
      padding-bottom: 1.25rem;
      border-bottom: 1px solid var(--cotai-border);
    }

    .eyebrow {
      margin: 0;
      color: var(--cotai-muted);
      font-size: .75rem;
      font-weight: 700;
      letter-spacing: .09em;
    }

    h1, h2, p { margin-top: 0; }

    .lead {
      margin: 0;
      color: var(--cotai-muted);
    }

    .panel {
      padding: clamp(1.25rem, 4vw, 2rem);
      border: 1px solid var(--cotai-border);
      border-radius: var(--cotai-radius-card);
      background: var(--cotai-surface, white);
      box-shadow: var(--cotai-shadow-card);
    }

    .status-line {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 1rem;
    }

    h2 {
      margin: .35rem 0 0;
      font-size: 1.4rem;
    }

    .pill {
      padding: .35rem .75rem;
      border-radius: 999px;
      background: #fef2f2;
      color: #991b1b;
      font-size: .8rem;
      font-weight: 700;
    }

    .pill.online {
      background: #dcfce7;
      color: #166534;
    }

    .message {
      margin: 1rem 0 1.5rem;
      color: var(--cotai-muted);
    }

    .message.error,
    .error {
      color: #991b1b;
    }

    .details {
      display: grid;
      grid-template-columns:
        repeat(auto-fit, minmax(14rem, 1fr));
      gap: 1rem;
    }

    .detail {
      display: grid;
      gap: .35rem;
      padding: 1rem;
      border-radius: .75rem;
      background: var(--cotai-background, #f8fafc);
    }

    .detail span {
      color: var(--cotai-muted);
      font-size: .8rem;
    }

    .detail strong {
      overflow-wrap: anywhere;
    }

    .scope {
      margin-top: 1.25rem;
      padding: 1rem;
      border-left: 3px solid var(--cotai-primary);
      background: var(--cotai-background, #f8fafc);
    }

    .scope p {
      margin: .4rem 0 0;
      color: var(--cotai-muted);
    }

    .actions {
      display: flex;
      flex-wrap: wrap;
      gap: .75rem;
      margin-top: 1.5rem;
    }

    .refresh {
      border: 0;
      border-radius: .65rem;
      padding: .75rem 1rem;
      background: var(--cotai-primary);
      color: white;
      font: inherit;
      font-weight: 700;
      cursor: pointer;
    }

    .refresh:disabled {
      opacity: .6;
      cursor: not-allowed;
    }

    .test-button {
      background: #166534;
    }

    .test-result {
      margin-top: 1.25rem;
      padding: 1rem;
      border-radius: .75rem;
      border: 1px solid;
    }

    .test-result p {
      margin: .5rem 0 0;
    }

    .test-success {
      background: #f0fdf4;
      border-color: #86efac;
      color: #166534;
    }

    .test-error {
      background: #fef2f2;
      border-color: #fca5a5;
      color: #991b1b;
    }

    .test-note {
      margin: 1rem 0 0;
      color: var(--cotai-muted);
      font-size: .85rem;
      line-height: 1.5;
    }

    .footnote {
      color: var(--cotai-muted);
      font-size: .85rem;
      line-height: 1.5;
    }
  `,
})
export class ProxyComponent {
  private readonly http = inject(HttpClient);

  protected readonly loading = signal(true);
  protected readonly testing = signal(false);

  protected readonly status = signal<ProxyStatus | null>(null);
  protected readonly error = signal('');
  protected readonly testResult = signal<ProxyTestResult | null>(null);

  constructor() {
    this.refresh();
  }

  protected refresh(): void {
    this.loading.set(true);
    this.error.set('');

    this.http.get<ProxyStatus>('/api/proxy/status').subscribe({
      next: (result) => {
        this.status.set(result);
        this.loading.set(false);
      },
      error: () => {
        this.status.set(null);
        this.error.set(
          'Verifique se o backend está em execução e se sua sessão continua válida.'
        );
        this.loading.set(false);
      },
    });
  }

  protected testConnection(): void {
    if (this.testing() || !this.status()?.enabled) {
      return;
    }

    this.testing.set(true);
    this.testResult.set(null);

    this.http.get<ProxyTestResult>('/api/proxy/test').subscribe({
      next: (result) => {
        this.testResult.set(result);
        this.testing.set(false);
      },
      error: (err: HttpErrorResponse) => {
        let message = 'Não foi possível completar a conexão externa.';

        if (err.status === 0) {
          message = 'Não foi possível comunicar com o backend.';
        } else if (err.status === 401) {
          message = 'Sua sessão expirou. Faça login novamente.';
        } else if (err.status === 403) {
          message = 'Você não tem permissão para executar este teste.';
        } else if (err.status === 502) {
          message = 'O backend não conseguiu completar a conexão externa pelo proxy.';
        }

        this.testResult.set({
          success: false,
          message,
        });

        this.testing.set(false);
      },
    });
  }
}
