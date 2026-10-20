# Checklist de QA visual

## Antes de aprovar uma tela

- [ ] Conferir com `references/identidade-aprovada.png` (paleta, tipografia, decoração leve, cards, tom).
- [ ] Não virou "app só de casamento": outros eventos também funcionam.
- [ ] Rodou em 320, 375, 430, 768, 1280px.
- [ ] Sem horizontal overflow (incluindo e-mails, preços altos e URLs longas).
- [ ] Botões, stepper de cotas e menu têm alvos >=44px.
- [ ] Contraste, foco, etiquetas de formulário, status textual e teclado conferidos.
- [ ] Dispositivos pequenos exibem preço, quantidade, disponibilidade e CTA corretamente.
- [ ] Página de pagamento oferece link alternativo ao QR e mostra o prazo real.
- [ ] Dados vêm da API e status correspondem ao ciclo real da reserva.
- [ ] Foram testados carregando, vazio, erro, indisponível, pendente, confirmado e expirado.
- [ ] Imagens adequadas ao contexto (churrasco, bodas, casamento etc.).

## Automação opcional

Playwright pode capturar screenshots nas cinco larguras. Ele só será configurado depois de examinar scripts e rotas reais do projeto, para **não colocar testes quebrados nem sobrescrever configuração existente**.

Guarde screenshots baseline *aprovadas por humanos* e faça comparações em CI. Diferenças devem gerar revisão, não ser aceitas automaticamente.
