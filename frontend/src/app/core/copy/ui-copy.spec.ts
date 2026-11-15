import { ReservationView } from '../models/reservation.model';
import { reservationCopy } from './ui-copy';

function view(status: ReservationView['status'], paymentStatus: string): ReservationView {
  return {
    reservationId: 'r1',
    itemName: 'Carvão',
    status,
    quantity: 1,
    amountCents: 45_00,
    expiresAt: '2026-11-15T12:30:00Z',
    payment: {
      paymentId: 'p1',
      status: paymentStatus,
      amountCents: 45_00,
      providerReference: 'ref-1',
      checkoutUrl: null,
      qrCodePayload: null,
      expiresAt: '2026-11-15T12:30:00Z',
    },
  };
}

describe('reservation copy', () => {
  it('follows the reservation status', () => {
    expect(reservationCopy(view('PENDING', 'PENDING')).badge).toBe('pending');
    expect(reservationCopy(view('CONFIRMED', 'PAID')).badge).toBe('confirmed');
    expect(reservationCopy(view('CANCELLED', 'DECLINED')).badge).toBe('cancelled');
    expect(reservationCopy(view('EXPIRED', 'EXPIRED')).badge).toBe('expired');
  });

  it('tells the guest when the payment arrived after the deadline', () => {
    const late = reservationCopy(view('EXPIRED', 'PAID'));
    expect(late.badge).toBe('expired');
    expect(late.detail).toContain('depois do prazo');
    expect(reservationCopy(view('CANCELLED', 'PAID')).badge).toBe('cancelled');
  });
});
