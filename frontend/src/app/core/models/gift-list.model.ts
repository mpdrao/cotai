export interface PublicItem {
  id: number;
  name: string;
  unit: string;
  unitPriceCents: number;
  totalQuotas: number;
  availableQuotas: number;
  imageUrl?: string;
}

export interface PublicGiftList {
  title: string;
  eventTypeCode: string;
  eventDate: string;
  status: 'DRAFT' | 'PUBLISHED' | 'CLOSED';
  acceptsReservations: boolean;
  items: PublicItem[];
}
