import { draftFrom, draftTotalCents, toItemInput, validateDraft } from './item-draft';

describe('item drafts', () => {
  it('accepts a valid item and converts it to the API shape', () => {
    const draft = { name: ' Picanha ', unit: 'kg', price: '90,00' };
    expect(validateDraft(draft)).toEqual({});
    expect(toItemInput(draft)).toEqual({ name: 'Picanha', unit: 'kg', unitPriceCents: 9000 });
  });

  it('totals one quota per item', () => {
    expect(draftTotalCents({ name: 'Picanha', unit: 'kg', price: '90,00' })).toBe(9000);
    expect(draftTotalCents({ name: 'Picanha', unit: 'kg', price: '' })).toBe(0);
  });

  it('flags every invalid field with the API limits', () => {
    const errors = validateDraft({ name: '', unit: '', price: '0' });
    expect(Object.keys(errors).sort()).toEqual(['name', 'price', 'unit']);
  });

  it('sends the stock photo of a suggestion, but not once the organizer picks a file', () => {
    const draft = draftFrom({ name: 'Picanha', unit: 'kg', unitPriceCents: 9000, imageUrl: '/suggestions/barbecue-1.jpg' });
    expect(toItemInput(draft).imageUrl).toBe('/suggestions/barbecue-1.jpg');

    draft.photo = new File(['x'], 'foto.jpg', { type: 'image/jpeg' });
    expect(toItemInput(draft).imageUrl).toBeUndefined();
  });

  it('never sends an uploaded photo URL as imageUrl', () => {
    const draft = draftFrom({ name: 'Picanha', unit: 'kg', unitPriceCents: 9000, imageUrl: '/api/item-photos/abc' });
    expect(toItemInput(draft).imageUrl).toBeUndefined();
  });
});
