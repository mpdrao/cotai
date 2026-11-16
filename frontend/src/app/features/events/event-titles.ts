import { EventType } from '../../core/models/event-type.model';

// Name ideas shown under the list title. Keys are the catalog codes (event_type.code in the API).
const TITLE_SUGGESTIONS: Record<string, string[]> = {
  BARBECUE: ['Churrasco da firma', 'Churras de fim de ano', 'Churrasco de aniversário', 'Resenha do fim de semana'],
  BIRTHDAY: ['Aniversário de 30 anos', 'Niver da Júlia', 'Festa surpresa', 'Aniversário do vovô'],
  BABY_SHOWER: ['Chá de bebê da Ana', 'Chá de fraldas', 'Chá revelação', 'Chegada do bebê'],
  WEDDING: ['Casamento de Ana e Pedro', 'Nosso casamento', 'Lua de mel dos noivos', 'Casa nova dos noivos'],
  OTHER: ['Bodas de prata', 'Formatura da turma', 'Chá de casa nova', 'Vaquinha da galera'],
};

export function titleSuggestionsFor(type: EventType | null): string[] {
  if (!type) return [];
  return TITLE_SUGGESTIONS[type.code] ?? TITLE_SUGGESTIONS['OTHER'];
}
