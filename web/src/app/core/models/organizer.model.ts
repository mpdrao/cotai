export type GiftListStatus = 'DRAFT' | 'PUBLISHED' | 'CLOSED';

export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

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
}

export interface ItemInput {
  name: string;
  unit: string;
  unitPriceCents: number;
  totalQuotas: number;
}

export interface Item extends ItemInput {
  id: number;
  totalCents: number;
  archived: boolean;
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
}
