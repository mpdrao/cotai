# Cota Ae!

Lista de presentes **com cotas** para qualquer evento (casamento, chá de bebê, churrasco…).

O organizador parte de 10 itens sugeridos por tipo de evento, ajusta nome, unidade, preço e número de cotas e publica um link. O convidado reserva cotas e paga por **QR code / link** num gateway simulado; um **webhook assinado** confirma a reserva automaticamente.

> A interface segue o design system Cota Ae! ([`docs/design-system/`](docs/design-system/), regras para agentes em [`AGENTS.md`](AGENTS.md)), mobile-first.
>
> Projeto de partida gerado a partir do design em [`docs/design/design-spec.md`](docs/design/design-spec.md) (diagramas de arquitetura, casos de uso, classes, estados, concorrência e edge cases) e do contrato [`docs/api/openapi.yaml`](docs/api/openapi.yaml).

## Stack

| Camada | Tecnologia |
| --- | --- |
| Backend | Java 21 · Spring Boot 4 · Spring Web MVC · Spring Data JPA · Spring Security (JWT) |
| Banco | PostgreSQL 17 · Flyway |
| Frontend | Angular 22 (standalone components, signals) |
| Contrato | OpenAPI 3.1 · erros em RFC 9457 (Problem Details) |
| Testes | JUnit 5 · Testcontainers (PostgreSQL real, não H2) |
| Build | Maven multi-módulo (`backend/` + `frontend/`), um único JAR em produção |

## Estrutura

```
cotai/
├── pom.xml                  # parent: módulos backend e frontend
├── docker-compose.yml       # postgres:17
├── backend/                 # Spring Boot (artifact cotai-api)
│   └── src/main/java/dev/cotai/
│       ├── organizer/       # Organizer, AuthController, JwtService
│       ├── eventtype/       # EventType, TemplateItem, EventTypeController
│       ├── giftlist/        # GiftList, Item, GiftListService, controllers
│       ├── reservation/     # Reservation, ReservationService, ReservationExpiryJob
│       ├── payment/         # Payment, PaymentGateway (porta), PaymentService, webhook
│       ├── mockgateway/     # @Profile("dev"): MockPaymentGateway, checkout com QR
│       ├── devsupport/      # @Profile("dev"): MutableClock, DevController
│       └── common/          # erros, segurança, Clock, helpers de hash
├── frontend/                # Angular (build empacotado em /static no JAR)
│   └── src/app/
│       ├── core/            # auth, models, services (ApiService, OrganizerService), copy, money
│       ├── features/        # uma pasta por tela: home, gift-list, reservation, auth, dashboard, lists
│       └── shared/cotai-ui/ # componentes do design system
└── docs/
    ├── api/                 # openapi.yaml + scripts/race-last-quota.sh
    ├── design/              # design-spec.md
    ├── design-system/       # marca, mobile-first, componentes, textos, a11y
    └── modelagem/           # modelo Astah (.asta)
```

## Como rodar (desenvolvimento)

Pré-requisitos: **Java 21**, **Node 24.15+** (para o Angular 22) e **Docker**.

```bash
# 1. Banco
docker compose up -d

# 2. API (perfil dev: gateway simulado + relógio controlável) em http://localhost:8080
./mvnw -DskipWeb -pl backend spring-boot:run

# 3. Frontend com proxy para a API em http://localhost:4200
cd frontend && npm install && npm start
```

Para o QR code funcionar no celular, aponte `PUBLIC_BASE_URL` para o IP da sua rede local (ex.: `http://192.168.0.10:8080`); com `localhost` a página não abre no aparelho.

### Fluxo de pagamento simulado

1. `POST /api/public/items/{itemId}/reservations` (header `Idempotency-Key` obrigatório) devolve `manageToken` e `payment.checkoutUrl`.
2. Abra o `checkoutUrl` (ou escaneie o QR) e clique em **Approve** ou **Decline**.
3. O gateway simulado chama `POST /api/webhooks/payments` com `X-Signature: sha256=HMAC(corpo cru)`.
4. A reserva vira `CONFIRMED` (ou `CANCELLED` se recusada). Pagamento após o prazo vira `REFUND_REQUIRED`.

Para reproduzir expirações sem esperar 30 minutos: `POST /dev/clock/advance {"minutes": 31}` e `POST /dev/jobs/expire-reservations`.

## Build e testes

```bash
./mvnw -DskipWeb -pl backend test      # testes unitários
./mvnw -DskipWeb -pl backend verify    # + integração (*IT) com Testcontainers (requer Docker)
./mvnw clean package               # JAR único com o Angular em /static
java -Dspring.profiles.active=dev -jar backend/target/cotai.jar
```

## Configuração

Variáveis de ambiente (valores padrão servem **apenas** para desenvolvimento):

| Variável | Uso |
| --- | --- |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | PostgreSQL |
| `JWT_SECRET` | chave HS256 (mín. 32 bytes) |
| `MANAGE_TOKEN_SECRET` | deriva o token de gerenciamento do convidado |
| `PAYMENT_WEBHOOK_SECRET` | chave HMAC do webhook |
| `PUBLIC_BASE_URL` | base dos links de checkout / QR |

> Aviso: Troque todos os segredos antes de qualquer ambiente real.

## Decisões de design que valem conhecer

- **Disponibilidade é derivada**, nunca persistida: `total − (CONFIRMED + PENDING não expiradas)`, calculada sob `SELECT … FOR UPDATE` na linha do item.
- **Toda transição de estado é um `UPDATE` condicional** (`WHERE status = 'PENDING'`): em corridas (pagar × expirar × cancelar) exatamente uma vence.
- **Nenhuma chamada externa com lock aberto**: a reserva faz commit e só então a cobrança é criada no gateway.
- **`Clock` injetado**: o código nunca usa `now()` direto; no perfil `dev` o relógio é controlável.
- **Webhook lê o corpo cru** (`byte[]`) para validar o HMAC.
- **`PaymentGateway` é uma porta**: trocar o simulador por um provedor real é implementar essa interface.

## Estado atual / próximos passos

Esta é a base inicial: o backend implementa os casos de uso do design (auth, listas, itens, reservas, pagamento simulado, webhook, expiração, resumo). Falta:

- [ ] Testes de integração de concorrência e webhook do design (`ReservationConcurrencyIT`, `IdempotencyRaceIT`, `ConfirmVsExpireRaceIT`, `CancelVsPaidRaceIT`, `PaymentWebhookIT`, `GatewayFailureIT`)
- [ ] Gerar os tipos do Angular a partir do `openapi.yaml`
- [ ] Collections Postman/Insomnia (`docs/api/collections/`)
- [ ] Rate limit por IP e verificação de e-mail (fora do escopo do design atual)
