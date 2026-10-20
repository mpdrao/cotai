export interface PaymentInfo {
  paymentId: string;
  status: string;
  amountCents: number;
  providerReference: string | null;
  checkoutUrl: string | null;
  qrCodePayload: string | null;
  expiresAt: string;
}

export interface ReservationCreated {
  reservationId: string;
  manageToken: string;
  status: string;
  quantity: number;
  amountCents: number;
  expiresAt: string;
  payment: PaymentInfo;
}

export interface ReservationView {
  reservationId: string;
  itemName: string;
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'EXPIRED';
  quantity: number;
  amountCents: number;
  expiresAt: string;
  payment: PaymentInfo;
}

export interface ReserveRequest {
  guestName: string;
  guestEmail: string;
  quantity: number;
}
