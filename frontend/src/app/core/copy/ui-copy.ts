import { HttpErrorResponse } from '@angular/common/http';
import { CotaiStatus } from '../../shared/cotai-ui';
import { ReservationView } from '../models/reservation.model';

/** Guest-facing messages per API error code (docs/design-system/UI_COPY.md). */
const ERROR_COPY: Record<string, string> = {
  QUOTA_UNAVAILABLE: 'Essa cota acabou de ser reservada. Atualizamos os números.',
  LIST_NOT_ACCEPTING_RESERVATIONS: 'Esta lista não está mais recebendo reservas.',
  TOO_MANY_PENDING_RESERVATIONS: 'Você já tem reservas aguardando pagamento nessa lista.',
  PAYMENT_GATEWAY_UNAVAILABLE: 'Não foi possível abrir o pagamento agora. Sua reserva foi cancelada; tente novamente.',
  VALIDATION_FAILED: 'Confira os dados informados e tente de novo.',
  INVALID_STATE_TRANSITION: 'Essa reserva não pode mais ser alterada.',
  EMAIL_ALREADY_REGISTERED: 'Já existe uma conta com esse e-mail. Tente entrar.',
  INVALID_CREDENTIALS: 'E-mail ou senha incorretos.',
  EVENT_DATE_IN_PAST: 'A data do evento já passou. Escolha hoje ou uma data futura.',
  LIST_HAS_NO_ITEMS: 'Adicione pelo menos um item antes de publicar.',
  QUOTAS_BELOW_OCCUPIED: 'Esse item já tem mais cotas reservadas do que o novo total.',
  ITEM_HAS_ACTIVE_RESERVATIONS: 'Esse item tem reservas ativas e não pode ser removido.',
  NOT_FOUND: 'Não encontramos o que você procurou.',
  PHOTO_INVALID: 'Use uma foto JPG, PNG ou WebP.',
  PHOTO_TOO_LARGE: 'A foto pode ter até 5 MB.',
};

/** Organizer screens: the same codes, with wording about the list instead of a reservation. */
export const ORGANIZER_COPY: Record<string, string> = {
  INVALID_STATE_TRANSITION: 'Essa lista não pode mais ser alterada nesse estado.',
  VALIDATION_FAILED: 'Confira os campos destacados e tente de novo.',
};

export function errorCode(error: unknown): string | undefined {
  return error instanceof HttpErrorResponse ? error.error?.code : undefined;
}

export function errorMessage(error: unknown, overrides: Record<string, string> = {}): string {
  const code = errorCode(error) ?? '';
  return overrides[code] ?? ERROR_COPY[code] ?? 'Não foi possível carregar. Tente novamente.';
}

export interface StatusCopy {
  badge: CotaiStatus;
  detail: string;
}

/** Reservation status as shown to the guest. A late payment is flagged on the payment, not the reservation. */
export function reservationCopy(view: ReservationView): StatusCopy {
  if (view.payment.status === 'REFUND_REQUIRED') {
    return {
      badge: 'refund',
      detail: 'O valor precisa de análise para estorno; a reserva não foi reaberta.',
    };
  }
  switch (view.status) {
    case 'CONFIRMED':
      return { badge: 'confirmed', detail: 'Sua contribuição foi registrada.' };
    case 'CANCELLED':
      return { badge: 'cancelled', detail: 'Essas cotas voltaram a ficar disponíveis.' };
    case 'EXPIRED':
      return { badge: 'expired', detail: 'Faça uma nova reserva se ainda houver cotas.' };
    default:
      return { badge: 'pending', detail: 'Pague dentro do prazo para garantir suas cotas.' };
  }
}
