export type GiftListStatus = 'DRAFT' | 'PUBLISHED' | 'CLOSED';

export interface ItemInput {
  name: string;
  unit: string;
  unitPriceCents: number;
  totalQuotas: number;

  imageUrl?: string;
}

export interface Item extends ItemInput {
  id: number;
  totalCents: number;
  archived: boolean;
  isDefault?: boolean;
}

export interface GiftList {
  id: string;
  title: string;
  slug: string;
  eventTypeCode: string;
  eventDate: string;
  status: GiftListStatus;
  reservationTtlMinutes: number;
  items: Item[];
  createdAt: string;
}

export interface GiftListSummary {
  id: string;
  title: string;
  slug: string;
  status: GiftListStatus;
  eventDate: string;
}

export interface ItemTotals {
  itemId: number;
  name: string;
  totalQuotas: number;
  confirmedQuotas: number;
  pendingQuotas: number;
  availableQuotas: number;
}

export interface Contributor {
  guestName: string;
  itemName: string;
  quantity: number;
  amountCents: number;
  confirmedAt: string;
}

export interface GiftListSummaryReport {
  giftListId: string;
  targetCents: number;
  confirmedCents: number;
  pendingCents: number;
  refundRequiredCents: number;
  items: ItemTotals[];
  contributors: Contributor[];
}

export interface CreateGiftListRequest {
  eventTypeCode: string;
  title: string;
  eventDate: string;
  reservationTtlMinutes?: number;
  items: ItemInput[];
}

export interface UpdateGiftListRequest {
  title?: string;
  eventDate?: string;
  reservationTtlMinutes?: number;
  eventTypeCode?: string;
}

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
  status: GiftListStatus;
  acceptsReservations: boolean;
  items: PublicItem[];
}
