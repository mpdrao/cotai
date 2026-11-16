import { PaidContribution } from '../../core/models/gift-list.model';

const HEADER = ['Data', 'Convidado', 'Item', 'Cotas', 'Valor (R$)', 'Mensagem'];
const FORMULA_START = /^[=+\-@\t\r]/;

/** Guest text opens in spreadsheets, so a leading formula character is neutralised before quoting. */
function cell(value: string): string {
  const safe = FORMULA_START.test(value) ? `'${value}` : value;
  return `"${safe.replace(/"/g, '""')}"`;
}

function reais(cents: number): string {
  return (cents / 100).toFixed(2).replace('.', ',');
}

export function contributionsCsv(rows: PaidContribution[]): string {
  const lines = rows.map((r) =>
    [
      cell(new Date(r.paidAt).toLocaleString('pt-BR')),
      cell(r.guestName),
      cell(r.itemName),
      String(r.quantity),
      reais(r.amountCents),
      cell(r.message ?? ''),
    ].join(';'),
  );
  return '﻿' + [HEADER.map(cell).join(';'), ...lines].join('\r\n') + '\r\n';
}
