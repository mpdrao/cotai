import { draftFrom, draftTotalCents, toItemInput, validateDraft } from './item-draft';

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

  it('sends the stock photo of a suggestion, but not once the organizer picks a file', () => {
    const draft = draftFrom({ name: 'Picanha', unit: 'kg', unitPriceCents: 9000, totalQuotas: 4, imageUrl: '/suggestions/barbecue-1.jpg' });
    expect(toItemInput(draft).imageUrl).toBe('/suggestions/barbecue-1.jpg');

    draft.photo = new File(['x'], 'foto.jpg', { type: 'image/jpeg' });
    expect(toItemInput(draft).imageUrl).toBeUndefined();
  });

  it('never sends an uploaded photo URL as imageUrl', () => {
    const draft = draftFrom({ name: 'Picanha', unit: 'kg', unitPriceCents: 9000, totalQuotas: 4, imageUrl: '/api/item-photos/abc' });
    expect(toItemInput(draft).imageUrl).toBeUndefined();
  });
});
