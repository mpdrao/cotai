import { NgModel } from '@angular/forms';

export type FieldMessages = Record<string, string>;

/**
 * Message for the first failed validator of a field, once the person has left it
 * (or tried to submit). Empty string while there is nothing to show.
 */
export function fieldError(field: Pick<NgModel, 'invalid' | 'touched' | 'errors'>, messages: FieldMessages): string {
  if (!field.invalid || !field.touched) return '';
  const failed = Object.keys(field.errors ?? {}).find((key) => key in messages);
  return failed ? messages[failed] : '';
}
