import { fieldError } from './field-error';

const messages = { required: 'Informe seu e-mail.', email: 'Confira o e-mail.' };

describe('fieldError', () => {
  it('stays quiet while the person has not left the field', () => {
    expect(fieldError({ invalid: true, touched: false, errors: { required: true } }, messages)).toBe('');
  });

  it('stays quiet for a valid field', () => {
    expect(fieldError({ invalid: false, touched: true, errors: null }, messages)).toBe('');
  });

  it('shows the message of the validator that failed', () => {
    expect(fieldError({ invalid: true, touched: true, errors: { email: true } }, messages)).toBe('Confira o e-mail.');
  });
});
