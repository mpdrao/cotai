# Frontend (Angular)

SPA do Cota Ae!. Visão geral do projeto, configuração e fluxo de pagamento: [`../README.md`](../README.md). Regras de UI: [`../AGENTS.md`](../AGENTS.md).

```bash
npm install
npm start        # http://localhost:4200, com proxy de /api e /mock-gateway para a API em :8080
npm test         # Vitest
npm run build    # saída em dist/web/browser (empacotada no JAR pelo Maven)
```

## Organização de `src/app/`

| Pasta | Conteúdo |
| --- | --- |
| `core/auth/` | `AuthService`, `authGuard`, `authInterceptor` |
| `core/models/` | Tipos da API (escritos à mão a partir de `docs/api/openapi.yaml`) |
| `core/services/` | `ApiService` (endpoints públicos) e `OrganizerService` (endpoints do organizador) |
| `core/copy/` | Textos de status e erro (`ui-copy.ts`) |
| `features/<tela>/` | Componentes, rotas e serviços de cada tela, sem subpastas |
| `shared/cotai-ui/` | Componentes do design system (`cotai-button`, `cotai-card`, ...) |
