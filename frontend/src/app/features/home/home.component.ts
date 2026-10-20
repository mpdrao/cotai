import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CotaiCardComponent, CotaiProgressComponent } from '../../shared/cotai-ui';

@Component({
  selector: 'app-home',
  imports: [RouterLink, CotaiCardComponent, CotaiProgressComponent],
  template: `
    <section class="hero" aria-labelledby="hero-title">
      <div class="hero-copy">
        <p class="cotai-eyebrow">Um encontro. Muitas mãos.</p>
        <h1 id="hero-title">Junta pra<br />fazer acontecer.</h1>
        <p class="cotai-lead">
          Uma lista, várias cotas e todo mundo junto.
          Organize o que seu evento precisa e compartilhe com quem vai fazer parte.
        </p>
        <div class="actions">
          <a routerLink="/lists/new" class="cotai-link-button cotai-link-button--primary">Criar minha lista <span aria-hidden="true">↗</span></a>
          <a href="#como-funciona" class="how-link">Ver como funciona <span aria-hidden="true">↓</span></a>
        </div>
        <ul class="events" aria-label="Tipos de evento">
          <li>Churrascos</li><li>Aniversários</li><li>Casamentos</li><li>E outros encontros</li>
        </ul>
      </div>

      <figure class="demo" aria-labelledby="demo-label">
        <cotai-card elevation="raised">
          <p id="demo-label" class="demo-label">Exemplo de lista</p>
          <p class="demo-title">Encontro da galera</p>
          <p class="demo-intro">Um pouco de cada um, um encontro de todos.</p>
          <ul class="demo-items">
            <li><div><p class="demo-name">Comes e bebes</p><p class="demo-price">R$ 35,00 <span>por cota</span></p></div><span class="demo-quantity">20 cotas</span></li>
            <li><div><p class="demo-name">Som e diversão</p><p class="demo-price">R$ 25,00 <span>por cota</span></p></div><span class="demo-quantity">10 cotas</span></li>
            <li><div><p class="demo-name">O toque final</p><p class="demo-price">R$ 15,00 <span>por cota</span></p></div><span class="demo-quantity">10 cotas</span></li>
          </ul>
          <div class="demo-progress">
            <div><span>Cotas reservadas</span><strong>16 de 40</strong></div>
            <cotai-progress [value]="40" label="Exemplo ilustrativo: cotas reservadas" />
            <p>Valores e progresso ilustrativos.</p>
          </div>
        </cotai-card>
        <div class="demo-note"><span aria-hidden="true">↗</span> Cada cota ajuda<br />o encontro a acontecer.</div>
      </figure>
    </section>

    <section id="como-funciona" class="steps" aria-labelledby="como-funciona-titulo">
      <div class="steps-heading">
        <p class="cotai-eyebrow">Da ideia ao encontro</p>
        <h2 id="como-funciona-titulo">Como funciona</h2>
      </div>
      <ol class="step-list">
        @for (step of steps; track step.title) {
          <li>
            <span class="step-number" aria-hidden="true">0{{ $index + 1 }}</span>
            <div><h3>{{ step.title }}</h3><p>{{ step.text }}</p></div>
          </li>
        }
      </ol>
    </section>
  `,
  styleUrl: './home.component.scss',
})
export class HomeComponent {
  protected readonly steps = [
    { title: 'Monte a lista', text: 'Escolha o tipo de evento, ajuste as sugestões de itens e defina as cotas.' },
    { title: 'Compartilhe o link', text: 'Cada pessoa escolhe como contribuir e reserva suas cotas, sem precisar criar conta.' },
    { title: 'Acompanhe tudo', text: 'O pagamento é feito por QR code ou link. Você acompanha as reservas e os pagamentos confirmados.' },
  ];
}
