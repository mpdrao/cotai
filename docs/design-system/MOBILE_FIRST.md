# Mobile-first — requisito obrigatório

**Projetar celular primeiro é regra de aceite.** Não criar layout desktop e adaptar por redução depois.

## Tamanhos de verificação

| Largura | Uso |
|---|---|
| **320px** | menor celular suportado |
| **375px** | baseline para construir e validar |
| **430px** | celular maior |
| **768px** | tablet: expansão progressiva |
| **1280px** | desktop: conteúdo ampliado |

Breakpoints definidos no kit: `40rem` (640px), `48rem` (768px), `64rem` (1024px). Use **apenas `min-width`** para aprimorar layouts, evitando regras contraditórias no mobile.

## Navegação

- Visitante: cabeçalho compacto, logo legível e menu acessível. Não criar sidebar que ocupe a tela pequena.
- Organizador: cards e métricas empilhados inicialmente; menu colapsável com rótulos e foco. Desktop pode usar sidebar fixa.
- Links compartilhados devem abrir direto a lista pública, sem exigir cadastro.

## Cards, formulários e pagamento

- Cards empilhados verticalmente; textos não podem estourar o card.
- Quantidade de cotas com botões `−` / `+` acessíveis, **44px** cada e limite de disponibilidade.
- CTA principal ocupa largura confortável (`width:100%` no mobile) e pode ficar visível no rodapé **sem cobrir conteúdo** quando isso fizer sentido.
- Nome, e-mail, preço total, prazo e status devem estar claros antes do pagamento.
- QR code precisa de link alternativo de pagamento e instrução compreensível em celular; nunca dependa de escanear o QR na mesma tela.
- Timer deve refletir o servidor; nunca prolongar reserva após expiração no frontend.
- Não crie links ou valores fictícios operacionais para gateway.

## Medidas mínimas

- Alvo de toque: 44×44 CSS px, com separação que evite erros.
- Texto de corpo padrão: 16px; descrição/suporte: preferir >=14px.
- Inputs: 44px de altura ou mais; teclado adequado (`inputmode`, `autocomplete`).
- Largura útil: `width:min(100% - 32px, 1140px)`.
- Imagens: `max-width:100%`, preferir `aspect-ratio` e `object-fit:cover`.
- Títulos podem quebrar linhas; evitar `white-space:nowrap` em texto longo.
- Animações discretas e adaptadas ao `prefers-reduced-motion`.

## Exemplo CSS

```css
.cotai-event-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 1rem;
}
@media (min-width: 40rem) {
  .cotai-event-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (min-width: 64rem) {
  .cotai-event-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
```

## Testes antes de aprovar

1. Em 320px, conferir navegação, cards, quantidades, formulário, CTA e QR/link.
2. Em 375 e 430px, verificar legibilidade sem zoom e sem scroll lateral.
3. Em 768 e 1280px, testar expansão sem imagens esticadas e sem conteúdo muito largo.
4. Percorrer fluxo completo **sem mouse** com teclado, e emulação de toque.
5. Conferir loading, erro, reservas encerradas, pagamento pendente, recusado e confirmado.
