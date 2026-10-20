import { ItemInput } from '../../core/models/organizer.model';
import { centsToInput, parseReais } from '../../core/money';

/** An item as the organizer types it: price is the text in the field ("90,00"). */
export interface ItemDraft {
  name: string;
  unit: string;
  price: string;
  totalQuotas: number | null;
}

export type ItemErrors = Partial<Record<keyof ItemDraft, string>>;

export function draftFrom(item: { name: string; unit: string; unitPriceCents: number; totalQuotas: number }): ItemDraft {
  return { name: item.name, unit: item.unit, price: centsToInput(item.unitPriceCents), totalQuotas: item.totalQuotas };
}

export function emptyDraft(): ItemDraft {
  return { name: '', unit: '', price: '', totalQuotas: 1 };
}

/** Same limits as ItemInput in the API (docs/api/openapi.yaml). */
export function validateDraft(d: ItemDraft): ItemErrors {
  const errors: ItemErrors = {};
  if (!d.name.trim()) errors.name = 'Informe o nome do item.';
  else if (d.name.trim().length > 80) errors.name = 'Use até 80 caracteres.';
  if (!d.unit.trim()) errors.unit = 'Informe a unidade (kg, caixa, pacote…).';
  else if (d.unit.trim().length > 30) errors.unit = 'Use até 30 caracteres.';
  const cents = parseReais(d.price);
  if (cents === null || cents < 1) errors.price = 'Informe um valor como 90,00.';
  else if (cents > 100_000_000) errors.price = 'O valor máximo por cota é R$ 1.000.000,00.';
  const q = d.totalQuotas;
  if (q === null || !Number.isInteger(q) || q < 1 || q > 1000) errors.totalQuotas = 'Use de 1 a 1000 cotas.';
  return errors;
}

export function toItemInput(d: ItemDraft): ItemInput {
  return { name: d.name.trim(), unit: d.unit.trim(), unitPriceCents: parseReais(d.price)!, totalQuotas: d.totalQuotas! };
}

export function draftTotalCents(d: ItemDraft): number {
  const cents = parseReais(d.price);
  return cents !== null && d.totalQuotas ? cents * d.totalQuotas : 0;
}
