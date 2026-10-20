# Revisão de composição — 20/10/2026

Implementação no frontend Angular, conferida com `references/identidade-aprovada.png`. Nome, slogan, Poppins, paleta, logo, rotas e contratos foram preservados. Nenhuma dependência foi acrescentada ao projeto e nenhum arquivo de backend foi alterado.

## Decisões visuais

- Hero com texto e demonstração de produto em duas colunas no desktop; título fluido, quebra intencional do slogan e CTA dominante. No celular, texto, ações e visual seguem o fluxo, sem sobreposição ou altura de viewport obrigatória.
- Demonstração identificada como “Exemplo de lista”, três itens e progresso ilustrativo, fundo pêssego orgânico e uma pequena sobreposição apenas no desktop. Não representa atividade, pagamento ou notificação real.
- “Como funciona” usa uma superfície suave e passos abertos, com conectores no desktop. Tipos de evento aparecem como texto auxiliar.
- Tema centraliza base sem sombra, superfície comum e destaque elevado. `cotai-card` tem elevação explícita; cards estáticos não se movimentam no hover.
- Dashboard separa saudação, título, ação e coleção; títulos das listas precedem data e status. Estados vazio, carregando e erro têm orientação, com nova tentativa de leitura em caso de erro.
- Lista pública tem cabeçalho tonalizado, preço por cota destacado e formulário integrado ao item selecionado. O foco vai ao formulário ao abrir e retorna ao botão ao voltar. Listas encerradas não convidam o usuário a reservar.
- Autenticação ganha composição própria no desktop. Criação e edição usam proximidade e espaçamento entre grupos; campos em uma coluna no celular. A reserva destaca o valor antes dos detalhes, preservando o estado e o prazo recebidos da API.

## Verificação realizada

| Verificação | Resultado |
| --- | --- |
| Build `npm run build` | Aprovado, sem avisos de orçamento |
| `npm test -- --watch=false` | 7 testes aprovados em 3 arquivos |
| Chromium: 320, 375, 430, 768 e 1280 px | Home, entrar, cadastro, dashboard, criação, gerenciamento, lista pública e reserva |
| Capturas antes/depois | 45 de cada versão, incluindo expansão do formulário de reserva |
| Overflow horizontal | Ausente nas telas verificadas e nos cenários de nomes longos sem espaços e valor alto |
| Axe-core | Nenhuma violação detectada nas 40 combinações de tela/largura, tags WCAG 2 A/AA, 2.1 AA e 2.2 AA |
| Alvos de interação | Controles habilitados e links medidos em pelo menos 44 × 44 px; radios avaliados por seus labels |
| Contraste calculado | Primário/branco 7,58:1; texto secundário/creme 5,93:1; borda de campo/branco 3,96:1 e borda/creme 3,74:1 |
| Teclado | CTA da home e redirecionamento pelo guard; tabulação no login; abertura, sequência e retorno de foco da reserva |
| Movimento reduzido | Transições reduzidas conforme `prefers-reduced-motion` |
| Estados e interações | 70 verificações concluídas, sem erros JavaScript |

Os estados incluíram dashboard vazio, carregando e erro com nova tentativa; lista pública vazia, encerrada, não encontrada, item indisponível e texto longo; reserva pendente, confirmada, cancelada, expirada e pagamento com estorno necessário. Foram conferidos também conflito de cotas, erro de login, validação de senha, validação da criação, payload de criação e edição, payload de reserva, chave de idempotência, armazenamento do token e navegação à reserva.

As chamadas da API foram interceptadas pelo Playwright com fixtures preparadas para esta verificação. Playwright, axe-core, os scripts e as fixtures foram usados fora do repositório e não fazem parte das dependências do projeto. A aplicação continuou usando seus serviços, formulários, rotas e estados existentes. O QR foi substituído por uma imagem identificada como omitida no teste. Não foram executados backend, gateway, webhook, pagamento real, VoiceOver/TalkBack, Safari ou Firefox. A auditoria automatizada não certifica conformidade integral de acessibilidade.

## Arquivos alterados

| Área | Arquivos |
| --- | --- |
| Tema e estrutura | `frontend/src/styles/cotai-theme.css`, `frontend/src/app/app.scss` |
| Componentes compartilhados | `frontend/src/app/shared/cotai-ui/cotai-card.component.ts`, `cotai-card.component.scss`, `cotai-button.component.scss`, `cotai-progress.component.scss`, `cotai-status-badge.component.scss` |
| Home | `frontend/src/app/features/home/home.component.ts`, `home.component.scss` |
| Autenticação | `frontend/src/app/features/auth/login.component.ts`, `register.component.ts`, `auth.component.scss` |
| Dashboard | `frontend/src/app/features/dashboard/dashboard.component.ts` |
| Lista pública | `frontend/src/app/features/gift-list/gift-list-detail.component.ts` |
| Criação e gerenciamento | `frontend/src/app/features/lists/list-new.component.ts`, `list-manage.component.ts`, `item-editor.component.ts` |
| Reserva | `frontend/src/app/features/reservation/reservation-detail.component.ts` |
| Documentação | `docs/design-system/COMPONENTS.md`, `docs/design/design-spec.md`, este relatório |
