import { describe, expect, it } from 'vitest';
import { PaidContribution } from '../../core/models/gift-list.model';
import { contributionsCsv } from './contributions-csv';

function row(overrides: Partial<PaidContribution> = {}): PaidContribution {
  return {
    reservationId: 'r1',
    guestName: 'Bia',
    itemId: 1,
    itemName: 'Carvão',
    quantity: 1,
    amountCents: 4500,
    paidAt: '2026-11-16T12:00:00Z',
    message: 'Parabéns!',
    ...overrides,
  };
}

describe('contributionsCsv', () => {
  it('writes a header and one line per contribution with comma decimals', () => {
    const lines = contributionsCsv([row()]).trim().split('\r\n');

    expect(lines).toHaveLength(2);
    expect(lines[0]).toContain('"Convidado";"Item"');
    expect(lines[1]).toContain('"Bia";"Carvão";1;45,00;"Parabéns!"');
  });

  it('doubles quotes and keeps separators inside the quoted message', () => {
    const csv = contributionsCsv([row({ message: 'Ele disse "oi"; tchau' })]);

    expect(csv).toContain('"Ele disse ""oi""; tchau"');
  });

  it('neutralises a message that would run as a spreadsheet formula', () => {
    const csv = contributionsCsv([row({ message: '=HYPERLINK("http://x")', guestName: '+cmd' })]);

    expect(csv).toContain(`"'=HYPERLINK(""http://x"")"`);
    expect(csv).toContain(`"'+cmd"`);
  });

  it('leaves the message empty when the guest wrote none', () => {
    const lines = contributionsCsv([row({ message: null })]).trim().split('\r\n');

    expect(lines[1].endsWith(';""')).toBe(true);
  });
});
