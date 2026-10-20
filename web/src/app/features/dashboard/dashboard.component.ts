import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CotaiCardComponent } from '../../shared/cotai-ui';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, CotaiCardComponent],
  template: `
    <h1>Área do organizador</h1>
    <cotai-card tinted>
      <h2>Em construção</h2>
      <p>
        As telas de entrar, criar lista a partir do tipo de evento, editar itens e acompanhar a arrecadação
        ainda vão ser feitas. A API dessas funções já existe.
      </p>
      <a routerLink="/">Voltar para o início</a>
    </cotai-card>
  `,
  styles: `
    h1 { margin: 0 0 1rem; font-size: clamp(1.5rem, 1.1rem + 2vw, 2.25rem); font-weight: 800; color: var(--cotai-primary); }
    h2 { margin: 0 0 0.5rem; font-size: 1.125rem; }
    p { margin: 0 0 1rem; color: var(--cotai-muted); }
    a { display: inline-flex; align-items: center; min-height: var(--cotai-tap-target); font-weight: 600; }
  `,
})
export class DashboardComponent {}
