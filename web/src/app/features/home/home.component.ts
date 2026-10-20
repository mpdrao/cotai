import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CotaiCardComponent } from '../../shared/cotai-ui';

@Component({
  selector: 'app-home',
  imports: [RouterLink, CotaiCardComponent],
  template: `
    <section class="hero">
      <h1 class="cotai-heading">Junta pra fazer <span class="accent">acontecer.</span></h1>
      <p class="cotai-lead">
        Listas com cotas para qualquer ocasião: churrasco, chá de bebê, casamento ou aquele encontro da galera.
        Cada pessoa reserva as cotas que quiser e paga por QR code ou link.
      </p>
      <div class="actions">
        <a routerLink="/dashboard" class="cta cta--primary">Criar minha lista</a>
        <a href="#como-funciona" class="cta cta--secondary">Ver como funciona</a>
      </div>
      <ul class="events" aria-label="Tipos de evento">
        <li>Churrasco</li>
        <li>Chá de bebê</li>
        <li>Casamento</li>
        <li>Outros encontros</li>
      </ul>
    </section>

    <section id="como-funciona" class="steps" aria-labelledby="como-funciona-titulo">
      <h2 id="como-funciona-titulo">Como funciona</h2>
      <ol class="cotai-grid cotai-grid--three step-list">
        @for (step of steps; track step.title) {
          <li>
            <cotai-card>
              <span class="step-number" aria-hidden="true">{{ $index + 1 }}</span>
              <h3>{{ step.title }}</h3>
              <p>{{ step.text }}</p>
            </cotai-card>
          </li>
        }
      </ol>
    </section>
  `,
  styles: `
    .hero { display: grid; gap: 1rem; padding-block: 0.5rem 2rem; }
    .hero h1 { margin: 0; color: var(--cotai-primary); }
    .accent { color: var(--cotai-terracotta); }
    .hero p { margin: 0; max-width: 38rem; }
    .actions { display: grid; gap: 0.75rem; }
    .cta {
      display: inline-flex; align-items: center; justify-content: center;
      min-height: var(--cotai-tap-target); padding: 0.7rem 1.25rem;
      border: 2px solid var(--cotai-primary); border-radius: var(--cotai-radius-pill);
      font-weight: 600; text-decoration: none; text-align: center;
    }
    .cta--primary { background: var(--cotai-primary); color: var(--cotai-surface); }
    .cta--secondary { background: var(--cotai-surface); color: var(--cotai-primary); }
    .events { display: flex; flex-wrap: wrap; gap: 0.5rem; margin: 0.5rem 0 0; padding: 0; list-style: none; }
    .events li {
      padding: 0.25rem 0.75rem; border-radius: var(--cotai-radius-pill);
      background: var(--cotai-surface); border: 1px solid var(--cotai-border);
      font-size: 0.875rem; color: var(--cotai-muted);
    }
    .steps h2 { margin: 0 0 1rem; font-size: 1.375rem; color: var(--cotai-primary); }
    .step-list { margin: 0; padding: 0; list-style: none; }
    .step-number {
      display: inline-grid; place-items: center; width: 2rem; height: 2rem;
      border-radius: 50%; background: var(--cotai-peach); color: var(--cotai-ink); font-weight: 700;
    }
    .steps h3 { margin: 0.75rem 0 0.25rem; font-size: 1.0625rem; }
    .steps p { margin: 0; color: var(--cotai-muted); }
    @media (min-width: 40rem) {
      .actions { display: flex; flex-wrap: wrap; }
    }
    @media (min-width: 64rem) {
      .hero { padding-block: 1.5rem 3rem; }
    }
  `,
})
export class HomeComponent {
  protected readonly steps = [
    {
      title: 'Monte a lista',
      text: 'Escolha o tipo de evento, ajuste as 10 sugestões de itens e defina quantas cotas cada um tem.',
    },
    {
      title: 'Compartilhe o link',
      text: 'Quem recebe o link escolhe os itens e reserva cotas, sem precisar criar conta.',
    },
    {
      title: 'Acompanhe tudo',
      text: 'O pagamento é feito por QR code ou link, e a reserva é confirmada sozinha quando ele chega.',
    },
  ];
}
