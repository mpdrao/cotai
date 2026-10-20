# Cota Ae! — instruções para agentes de programação

> **Obrigatório para UI/UX:** leia `docs/design-system/BRAND_GUIDELINES.md`, `MOBILE_FIRST.md`, `COMPONENTS.md`, `ACCESSIBILITY.md` e `INTEGRATION.md` antes de criar ou alterar interfaces. Se for incompatível com as instruções existentes do projeto, sinalize o conflito ao responsável; nunca descarte regras de segurança e testes.

## Fonte de verdade visual

1. Produto e nome público: **Cota Ae!** (com espaço e exclamação), slogan **"Junta pra fazer acontecer."**
2. Referência primária: `docs/design-system/references/identidade-aprovada.png`.
3. Tokens de estilo: `web/src/styles/_design-tokens.scss` e `web/src/styles/cotai-theme.css`.
4. Componentes reutilizáveis: `web/src/app/shared/cotai-ui/`.
5. O quadro aprovado contém lettering desenhado à mão. Os SVGs deste kit são interpretativos, NÃO definitivos; não alegue que estão vetorizados com fidelidade perfeita.
6. Preserve o conceito: fácil organizar e contribuir com **cotas** em churrascos, aniversários, bodas, casamentos, chás de bebê e outros eventos. Evite branding exclusivamente romântico, nupcial ou infantil.

## MOBILE FIRST — obrigatório, não opcional

- Inicie o CSS pelo celular **sem media queries**; utilize apenas `@media (min-width: ...)` para expandir.
- Projete e teste primeiro em 320px, 375px e 430px, depois 768px e 1280px.
- Botões, inputs e ações tocáveis: pelo menos **44×44 CSS px**; forneça foco visível e labels acessíveis.
- Uma coluna em celular para listas/formulários; duas ou mais colunas só quando houver espaço real.
- Não use tamanhos fixos que forcem overflow; use `min-width:0`, `max-width:100%`, `overflow-wrap` e imagens responsivas.
- A experiência do convidado é especialmente mobile-first: acessar link → escolher cotas → reservar → pagar → verificar status.
- O dashboard do organizador também deve operar em celular, sem exigir tela grande. Sidebar vira menu acessível ou navegação compacta.
- Não esconda funções críticas no mobile, não reduza alvos de toque e não use hover como único meio de interação.

## Regras da identidade

- Use exclusivamente os tokens aprovados: verde `#2E5D46`, verde suave `#6BA77A`, creme `#FFF7EF`, pêssego `#F4B198`, terracota `#D96B5B`. Para texto, use verde profundo `#16372A`; tokens adicionais estão documentados.
- Fonte de interface: **Poppins** (com fallbacks). O logo não precisa usar Poppins: sua aparência manual é uma característica separada.
- Prefira superfícies creme/brancas, botões pílula, cards de cantos suaves, ilustrações discretas e destaques terracota.
- Use `var(--cotai-...)` em vez de hexadecimais avulsos no código de componentes.
- Nunca redesenhe o logo, mude a marca, introduza uma nova paleta ou utilize caixas de presente/corações como símbolo **obrigatório** da plataforma sem aprovação.
- Adapte imagens e ícones ao tipo de evento; preserve os mesmos componentes, cores e navegação.
- Acessibilidade e semântica são superiores à decoração: contraste AA, estados de foco, mensagens de erro explícitas e textos claros.

## Arquitetura e comportamento

- Frontend Angular existente e backend Spring Boot; **não renomeie pacotes, rotas, entidades, endpoints nem campos da API** apenas porque a marca mudou.
- Componentes Angular devem ser standalone onde compatível, reutilizáveis e com estilos isolados.
- Use estados reais da API e do pagamento: `PENDING`, `CONFIRMED`, `CANCELLED`, `EXPIRED`, e pendências de estorno quando aplicável. Não invente confirmação, valores, disponibilidade, sucesso de pagamento ou QR válido.
- Não implemente pagamento real, marketplace ou novos fluxos sem requisito explícito.
- Reaproveite padrões existentes; faça a menor mudança necessária e explique incompatibilidades.
- Nunca sobrescreva configurações existentes sem inspecioná-las.

## Como o design system já está aplicado neste repositório

Toda tela nova ou alterada continua seguindo este padrão; não crie estilos paralelos.

- **Tema ativo:** `web/src/styles.scss` faz `@use 'styles/cotai-theme';` e `web/src/app/app.html` envolve tudo em `<div class="cotai-app">`. Não mude isso nem adicione outro CSS global.
- **Componentes:** use os de `web/src/app/shared/cotai-ui/` (`cotai-button`, `cotai-card`, `cotai-status-badge`, `cotai-progress`). Precisou de um componente novo? Crie ali, standalone, com estilos via `var(--cotai-...)`.
- **Cores:** só `var(--cotai-...)`. Faltou uma cor? Adicione o token em `web/src/styles/cotai-theme.css`, nunca um hex dentro do componente.
- **Textos:** status e erros da API passam por `web/src/app/core/copy/ui-copy.ts`, com o texto de `docs/design-system/UI_COPY.md`. Não mostre `code`, `detail` cru, token ou chave de idempotência.
- **Marca:** logos em `web/public/brand/` (servidos em `/brand/...`). O nome na interface é sempre **Cota Ae!**.
- **Locale:** `pt-BR` já está configurado em `app.config.ts`; use `currency: 'BRL'` e `date` para valores e prazos.
- **Páginas servidas pela API** (ex.: `api/src/main/resources/templates/mock-checkout.html`): repetem os mesmos tokens num `<style>` próprio, porque não carregam o CSS do Angular. Deixe claro que o pagamento é simulado.
- **Referência pronta:** `features/home`, `features/gift-list/gift-list-detail` e `features/reservation/reservation-detail` já seguem o padrão; copie a estrutura deles para telas novas, como as do organizador (`/login`, `/register`, `/lists/new`, `/lists/:id`).
- **Sem scripts de ativação ou geração.** Mudanças de visual são feitas direto no código.
- **Documentação:** se uma mudança alterar comportamento descrito em `docs/design/design-spec.md`, atualize o arquivo, inclusive a seção "Divergências conhecidas".

## Critério de aceite para qualquer tela nova ou modificada

- [ ] Design conferido com a imagem aprovada.
- [ ] CSS mobile-first (320, 375, 430, 768, 1280px).
- [ ] Sem scroll horizontal inesperado.
- [ ] Toques >=44px, teclado e foco visível.
- [ ] Uso de tokens e componentes existentes.
- [ ] Estilos e conteúdo adequados para todos os tipos de evento.
- [ ] Estados de carregamento, vazio, erro, indisponível e sucesso.
- [ ] Preço em R$, cotas e prazos exibidos corretamente.
- [ ] Mudanças visualmente verificadas (screenshots ou testes quando disponíveis).

Se algum requisito visual exigir alteração de padrão, **pergunte antes de executar**.
