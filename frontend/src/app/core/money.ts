/** "90,00", "1.234,5", "90.5" or "90" -> cents. null when it is not a valid amount. */
export function parseReais(value: string | number | null | undefined): number | null {
  let s = String(value ?? '').trim().replace(/^R\$\s*/, '').replace(/\s/g, '');
  if (s.includes(',')) {
    s = s.replace(/\./g, '').replace(',', '.');
  } else if (!/^\d+\.\d{1,2}$/.test(s)) {
    s = s.replace(/\./g, '');
  }
  if (!/^\d+(\.\d{1,2})?$/.test(s)) {
    return null;
  }
  return Math.round(parseFloat(s) * 100);
}

/** Cents -> "90,00" for an editable input. */
export function centsToInput(cents: number): string {
  return (cents / 100).toFixed(2).replace('.', ',');
}
