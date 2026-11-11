const MESSAGES: readonly (readonly [string, string, string])[] = [
  [
    'Tudo pronto pra começar: {p}% arrecadado até agora.',
    '{p}% arrecadado. Toda vaquinha começa assim: faltam {r}%.',
    'A largada foi dada: {p}% até agora, {r}% pela frente.',
  ],
  [
    '{p}% já foi, faltam {r}%.',
    'Primeiras cotas pagas! Já são {p}% da meta.',
    'Começou a andar: {p}% arrecadado.',
  ],
  [
    '{p}% arrecadado. A galera está chegando.',
    'Já dá pra sentir o clima: {p}% da meta.',
    '{p}% já foi, {r}% pela frente.',
  ],
  [
    '{p}% da meta. Bom ritmo!',
    'Passou dos 30%! Faltam {r}%.',
    'Um terço do caminho: {p}% arrecadado.',
  ],
  [
    '{p}% arrecadado. A metade já está à vista.',
    'Chegando na metade: faltam {r}%.',
    'Mais um empurrão e passa da metade. Já são {p}%.',
  ],
  [
    'Metade do caminho! {p}% arrecadado.',
    'Passou da metade: {p}% da meta.',
    '{p}% já foi. Agora é ladeira abaixo.',
  ],
  [
    '{p}% arrecadado. Faltam só {r}%.',
    'A maior parte já veio: {p}% da meta.',
    'Tá rendendo! {p}% arrecadado.',
  ],
  [
    '{p}% da meta. Já dá pra sentir o cheiro do encontro.',
    'Faltam só {r}% pra fechar a conta.',
    'Mais de dois terços: {p}% arrecadado.',
  ],
  [
    '{p}%, vocês estão quase lá!',
    'Reta final: faltam {r}%.',
    '{p}% arrecadado. Só mais umas cotas.',
  ],
  [
    'Falta pouquinho: só {r}%!',
    '{p}%! Mais umas cotas e a meta fecha.',
    'Quase, quase: {p}% da meta.',
  ],
  [
    'Meta batida! Junta pra fazer acontecer: aconteceu.',
    '100% arrecadado. Agora é só aproveitar o encontro.',
    'Conseguiram! Todas as cotas foram pagas.',
  ],
];

export interface CollectionProgress {

  percent: number;
  message: string;
}

export function collectionProgress(confirmedCents: number, targetCents: number, seed: string): CollectionProgress {
  const exact = targetCents > 0 ? (confirmedCents / targetCents) * 100 : 0;

  const percent = Math.min(100, exact > 0 ? Math.max(1, Math.floor(exact)) : 0);
  const band = Math.min(10, Math.floor(percent / 10));
  const message = MESSAGES[band][hash(`${seed}:${band}`) % 3]
    .replace('{p}', String(percent))
    .replace('{r}', String(100 - percent));
  return { percent, message };
}

function hash(text: string): number {
  let h = 0;
  for (const c of text) h = (h * 31 + c.charCodeAt(0)) >>> 0;
  return h;
}

export const PROGRESS_MESSAGES = MESSAGES;
