import { draftTotalCents, toItemInput, validateDraft } from './item-draft';

describe('item drafts', () => {
  it('accepts a valid item and converts it to the API shape', () => {
    const draft = { name: ' Picanha ', unit: 'kg', price: '90,00', totalQuotas: 4 };
    expect(validateDraft(draft)).toEqual({});
    expect(toItemInput(draft)).toEqual({ name: 'Picanha', unit: 'kg', unitPriceCents: 9000, totalQuotas: 4 });
    expect(draftTotalCents(draft)).toBe(36000);
  });

  it('flags every invalid field with the API limits', () => {
    const errors = validateDraft({ name: '', unit: '', price: '0', totalQuotas: 1001 });
    expect(Object.keys(errors).sort()).toEqual(['name', 'price', 'totalQuotas', 'unit']);
  });
});
