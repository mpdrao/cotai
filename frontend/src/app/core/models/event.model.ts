export interface Event {
  code: string;
  name: string;
  defaultReservationTtlMinutes: number;
  event?: Event | null;
  eventCode?: string | null;
  giftListId?: string | null;
}

