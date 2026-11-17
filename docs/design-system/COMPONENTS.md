# Componentes e padrões de interface

## Reutilização

Componentes Angular standalone iniciais em `frontend/src/app/shared/cotai-ui/`:

| Nome | Seletor | Uso |
|---|---|---|
| `CotaiButtonComponent` | `<cotai-button>` | CTA primário, secundário e discreto; estado de carregamento |
| `CotaiCardComponent` | `<cotai-card>` | Superfícies e painéis |
| `CotaiStatusBadgeComponent` | `<cotai-status-badge>` | Status de reserva e de pagamento |
| `CotaiProgressComponent` | `<cotai-progress>` | Ocupação/porcentagem das cotas |

Eles **não são acoplados a rotas** e não substituem a lógica já existente. São ponto de partida coerente com a identidade aprovada; adapte APIs para o projeto quando necessário.

### Superfícies e profundidade

O tema existente centraliza os níveis: base creme sem sombra (`--cotai-shadow-base`), superfície branca ou tonalizada (`--cotai-surface-sage`, `--cotai-surface-peach`) e destaque (`--cotai-shadow-raised`). `--cotai-shadow-card` é a sombra suave para uso localizado.

`cotai-card` recebe `elevation="flat"` (padrão), `"soft"` ou `"raised"`. Use `raised` para o painel ilustrativo do hero; cards comuns ficam em `flat`. `tinted` aplica a superfície suave de verde sem borda contrastante. Os raios são 12 px nos campos, 20 px em cards e 28 px nos destaques. Os tokens `--cotai-card-padding`, `--cotai-card-border` e `--cotai-card-radius` permitem ajustes locais no host sem atravessar o encapsulamento Angular.

Cards estáticos não se movem no hover. Links e botões têm transição de 180 ms, foco visível verde e respeitam movimento reduzido. A lista selecionada para reserva usa borda e sombra suave, com foco no início do formulário ao abrir e retorno ao botão ao voltar.

### Exemplo Angular

```ts
import { Component } from '@angular/core';
import { CotaiButtonComponent, CotaiCardComponent, CotaiProgressComponent } from './shared/cotai-ui';

@Component({
  selector: 'app-example',
  standalone: true,
  imports: [CotaiButtonComponent, CotaiCardComponent, CotaiProgressComponent],
  template: `
    <cotai-card>
      <h2>Churrasco da Firma</h2>
      <p>12 de 20 cotas reservadas</p>
      <cotai-progress [value]="60" label="Cotas reservadas" />
      <cotai-button (pressed)="openList()">Ver lista</cotai-button>
    </cotai-card>
  `,
})
export class ExampleComponent {
  openList(): void { /* navegar usando Router existente */ }
}
```

## Comportamento dos controles

- Botões só podem se parecer clicáveis se executarem ação. `disabled` desabilita nativamente.
- Badges exibem rótulo de status, **não apenas cor**.
- Barra de progresso informa valor com `role=progressbar` e `aria-valuenow`.
- Campos exigem rótulos visíveis, mensagens de ajuda e erros junto ao campo.
- Diálogos bloqueantes: foco contido, Escape quando aplicável, retorno ao gatilho.
- QR de pagamento: mostrar valor, prazo, botão de abrir checkout e status real. Não gerar QR de exemplo para cobrança real.
- Listas vazias: CTA orientando o próximo passo.

## Páginas prioritárias

**Convidado**: página pública de lista, detalhe de item, reservar cotas, checkout via link/QR simulado, minha reserva/status.

**Organizador**: entrar/cadastrar, minhas listas, criar lista a partir de modelo, editar itens, publicar/compartilhar, dashboard com status de cotas, fechar lista.

## Responsividade

Todo componente deve funcionar como padrão em 320px; só depois ampliar com `@media (min-width:40rem)`, `48rem`, `64rem`. Leia `MOBILE_FIRST.md` antes de implementar.
