import { PROGRESS_MESSAGES, collectionProgress } from './progress-copy';

describe('collection progress', () => {
  it('shows the exact percentage, not the band', () => {
    expect(collectionProgress(37_00, 100_00, 'a').percent).toBe(37);
    expect(collectionProgress(9_99, 100_00, 'a').percent).toBe(9);
  });

  it('never shows 100% before the goal, and shows 1% once something is paid', () => {
    expect(collectionProgress(99_99, 100_00, 'a').percent).toBe(99);
    expect(collectionProgress(1, 100_00, 'a').percent).toBe(1);
    expect(collectionProgress(0, 100_00, 'a').percent).toBe(0);
    expect(collectionProgress(0, 0, 'a').percent).toBe(0);
    expect(collectionProgress(120_00, 100_00, 'a').percent).toBe(100);
  });

  it('has three variations for every band of 10%, filled with the real numbers', () => {
    expect(PROGRESS_MESSAGES).toHaveLength(11);
    PROGRESS_MESSAGES.forEach((band) => expect(new Set(band).size).toBe(3));
    const { message } = collectionProgress(37_00, 100_00, 'a');
    expect(message).not.toMatch(/\{[pr]\}/);
    expect(PROGRESS_MESSAGES[3].map((m) => m.replace('{p}', '37').replace('{r}', '63'))).toContain(message);
  });

  it('keeps the message inside a band and changes it when the band changes', () => {
    const at31 = collectionProgress(31_00, 100_00, 'list-1').message;
    const at38 = collectionProgress(38_00, 100_00, 'list-1').message;
    const template = (pct: number, msg: string) => msg.replace(String(pct), '{p}').replace(String(100 - pct), '{r}');
    expect(template(31, at31)).toBe(template(38, at38));
    expect(PROGRESS_MESSAGES[4].map((m) => m.replace('{p}', '41').replace('{r}', '59')))
      .toContain(collectionProgress(41_00, 100_00, 'list-1').message);
  });

  it('uses every variation across lists', () => {
    const used = new Set(Array.from({ length: 30 }, (_, i) => collectionProgress(55_00, 100_00, `list-${i}`).message));
    expect(used.size).toBe(3);
  });
});
