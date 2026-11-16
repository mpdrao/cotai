import { titleSuggestionsFor } from './event-titles';

describe('title suggestions by event type', () => {
  it('has nothing to suggest before a type is chosen', () => {
    expect(titleSuggestionsFor(null)).toEqual([]);
  });

  it('suggests birthday names for a birthday', () => {
    const titles = titleSuggestionsFor({ code: 'BIRTHDAY', name: 'Aniversário', defaultReservationTtlMinutes: 30 });
    expect(titles).toContain('Aniversário de 30 anos');
  });

  it('falls back to the generic ideas for a type the front does not know yet', () => {
    const titles = titleSuggestionsFor({ code: 'GRADUATION', name: 'Formatura', defaultReservationTtlMinutes: 30 });
    expect(titles).toEqual(titleSuggestionsFor({ code: 'OTHER', name: 'Outro', defaultReservationTtlMinutes: 30 }));
  });
});
