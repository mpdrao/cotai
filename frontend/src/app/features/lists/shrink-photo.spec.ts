import { shrinkPhoto } from './shrink-photo';

describe('shrinkPhoto', () => {
  it('keeps the original file when the browser cannot decode it', async () => {
    const file = new File(['not really an image'], 'foto.png', { type: 'image/png' });
    expect(await shrinkPhoto(file)).toBe(file);
  });
});
