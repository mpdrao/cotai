import { centsToInput, parseReais } from './money';

describe('parseReais', () => {
  it('reads Brazilian and plain formats as cents', () => {
    expect(parseReais('90,00')).toBe(9000);
    expect(parseReais('1.234,5')).toBe(123450);
    expect(parseReais('R$ 12,34')).toBe(1234);
    expect(parseReais('90.5')).toBe(9050);
    expect(parseReais('90')).toBe(9000);
    expect(parseReais('1.000')).toBe(100000);
  });

  it('rejects what is not an amount', () => {
    expect(parseReais('')).toBeNull();
    expect(parseReais('abc')).toBeNull();
    expect(parseReais('1,234')).toBeNull();
    expect(parseReais('-5')).toBeNull();
  });

  it('formats cents for an editable input', () => {
    expect(centsToInput(9000)).toBe('90,00');
    expect(centsToInput(1250)).toBe('12,50');
  });
});
