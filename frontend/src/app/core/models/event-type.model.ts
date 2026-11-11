export interface EventType {
  code: string;
  name: string;
  defaultReservationTtlMinutes: number;
}

export interface TemplateItem {
  position: number;
  name: string;
  unit: string;
  unitPriceCents: number;
  suggestedQuotas: number;
  imageUrl?: string;
}
