# Acessibilidade — padrões mínimos

- WCAG 2.2 AA como meta de implementação (verificar caso a caso).
- Contraste mínimo 4.5:1 para texto normal e 3:1 para texto grande e elementos visuais essenciais.
- **Branco sobre `#2E5D46`**: contraste confortável; terracota como texto pequeno sobre branco não atende 4.5:1.
- Não depender de cor, ícone ou contagem regressiva visual isoladamente para expressar um status.
- Botões e links com 44px de alvo interativo como padrão interno (maior que alguns mínimos normativos).
- Inputs com `<label>`, tipos adequados e `aria-describedby` para mensagens.
- Ordem do teclado e foco visível; sem armadilha de teclado em navegação.
- Usar `aria-live` moderadamente para alterações de status importantes, sem anúncios repetitivos a cada segundo do timer.
- `prefers-reduced-motion: reduce` para transições.
- Imagem decorativa com `alt=""`; imagem informativa com descrição útil.
- QR acompanhado de link ou alternativa clicável para pagamento no mesmo celular.
- Testar VoiceOver/TalkBack quando possível e pelo menos teclado + auditoria automatizada.
