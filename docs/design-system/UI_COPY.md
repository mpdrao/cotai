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

## Progresso da arrecadação

- Mostre a porcentagem exata confirmada da meta (ex.: **37% da meta confirmada**). Nunca arredonde para 100% antes de bater a meta; com qualquer valor pago abaixo de 1%, mostre 1%.
- Abaixo da barra, uma frase de incentivo que muda a cada faixa de 10% (0–9, 10–19, …, 90–99, 100), com três variações por faixa. A variação é fixa por lista para não trocar a cada visita.
- Os textos ficam em `frontend/src/app/core/copy/progress-copy.ts`. Exemplos: "{p}% já foi, faltam {r}%." · "Reta final: faltam {r}%." · "Meta batida! Junta pra fazer acontecer: aconteceu."

## Mensagens de erro

- `QUOTA_UNAVAILABLE`: **Essa cota acabou de ser reservada.** Atualizamos os números.
- `LIST_NOT_ACCEPTING_RESERVATIONS`: **Esta lista não está mais recebendo reservas.**
- `TOO_MANY_PENDING_RESERVATIONS`: **Você já tem reservas aguardando pagamento nessa lista.**
- `PAYMENT_GATEWAY_UNAVAILABLE`: **Não foi possível abrir o pagamento agora.** Sua reserva foi cancelada; tente novamente.
- `PHOTO_INVALID`: **Use uma foto JPG, PNG ou WebP.**
- `PHOTO_TOO_LARGE`: **A foto pode ter até 5 MB.**
- Foto não enviada ao criar a lista: **Lista criada, mas 1 foto não foi enviada.** Adicione de novo no item.
- Erro genérico de rede: **Não foi possível carregar.** Tentar novamente.

Não exponha `manageToken`, chaves de idempotência nem detalhes internos da API na interface pública.
