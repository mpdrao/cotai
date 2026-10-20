# Integração com Angular e Spring Boot

## O que o ZIP não muda

Não modifica `pom.xml`, `angular.json`, rotas, `package.json`, autenticação, API, gateway, nem código Java. A marca pode mudar sem renomear os pacotes Java do projeto original.

## Ativação do CSS

Já ativado neste repositório: `frontend/src/styles.scss` faz `@use 'styles/cotai-theme';` e `frontend/src/app/app.html` envolve a aplicação em `<div class="cotai-app">`. Não é preciso rodar nada.

## Fonte Poppins

Uma opção é usar os arquivos de fonte por meio do serviço de fonte escolhido pelo projeto, incluindo pesos 400, 500, 600, 700 e 800; este pacote **não distribui arquivos de fonte**. Para desenvolvimento rápido, no `<head>` de `frontend/src/index.html`:

```html
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Poppins:wght@400;500;600;700;800&display=swap" rel="stylesheet">
```

Para produção, avalie auto-hospedar com licenças adequadas e estratégia de desempenho. A interface tem fallbacks enquanto a fonte não carrega.

## Assets e componentes

- Logo: `frontend/public/brand/logo-primary.svg`; outra opção `logo-on-dark.svg`; ícone `app-icon.svg`.
- Importar componentes standalone de `frontend/src/app/shared/cotai-ui/index.ts` e usar conforme `COMPONENTS.md`.
- **Os SVGs são interpretações**; o logo tipográfico exato aprovado está na **arte de referência** e requer finalização/vetorização, se necessário.
- Se `frontend/src/assets` não for publicado na versão de Angular instalada, conferir `angular.json`; algumas versões usam `public/` como padrão. Nesse caso, copie os assets para a pasta configurada ou ajuste o build explicitamente.

## Testes sugeridos

- Build Angular: execute o comando `npm run build` que existir no `frontend/package.json` do seu repo.
- Screenshots de rota real em 320, 375, 430, 768 e 1280px.
- Testar fluxo de convidado (página pública, cotas, reserva, pagamento simulado, atualização de status) e organizador (criar/editar/publicar, indicadores).
- Verificar dados reais e os códigos de status existentes da API. Um pagamento simulado não deve se apresentar como integração real de produção.

## Importante para o agente

Descompactar fornece as referências e código base. **Não integra nem migra sozinho todas as páginas existentes**. O próximo passo é conectar gradualmente os componentes às telas e fluxos reais.
