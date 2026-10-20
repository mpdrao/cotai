# Linguagem e microcopy (pt-BR)

Use português do Brasil, frase curta, sem exagerar em termos financeiros ou prometer segurança de um gateway que ainda é simulado.

## Ações

| Contexto | Texto preferido |
|---|---|
| Criar | Criar minha lista |
| Convidar | Compartilhar lista |
| Ver lista | Ver cotas |
| Reservar | Reservar minhas cotas |
| Pagar | Ir para pagamento |
| Ver reserva | Acompanhar minha reserva |
| Encerrar | Encerrar lista |

## Estados

- `PENDING`: **Aguardando pagamento** — Você tem até [horário real] para pagar.
- `CONFIRMED`: **Pagamento confirmado** — Sua contribuição foi registrada.
- `CANCELLED`: **Reserva cancelada** — Essas cotas voltaram a ficar disponíveis.
- `EXPIRED`: **Tempo esgotado** — Faça uma nova reserva se ainda houver cotas.
- `REFUND_REQUIRED` (pagamento): **Pagamento fora do prazo** — O valor precisa de análise para estorno; não reabrir a reserva.

## Mensagens de erro

- `QUOTA_UNAVAILABLE`: **Essa cota acabou de ser reservada.** Atualizamos os números.
- `LIST_NOT_ACCEPTING_RESERVATIONS`: **Esta lista não está mais recebendo reservas.**
- `TOO_MANY_PENDING_RESERVATIONS`: **Você já tem reservas aguardando pagamento nessa lista.**
- `PAYMENT_GATEWAY_UNAVAILABLE`: **Não foi possível abrir o pagamento agora.** Sua reserva foi cancelada; tente novamente.
- Erro genérico de rede: **Não foi possível carregar.** Tentar novamente.

Não exponha `manageToken`, chaves de idempotência nem detalhes internos da API na interface pública.
