import { Component, Input } from '@angular/core';

export type CotaiStatus = 'pending' | 'confirmed' | 'cancelled' | 'expired' | 'refund' | 'draft' | 'published' | 'closed';

@Component({
  selector: 'cotai-status-badge',
  standalone: true,
  template: `<span class="cotai-status" [class]="'cotai-status cotai-status--' + status">{{ text }}</span>`,
  styleUrls: ['./cotai-status-badge.component.scss'],
})
export class CotaiStatusBadgeComponent {
  @Input() status: CotaiStatus = 'pending';
  get text(): string {
    return {
      pending: 'Aguardando pagamento',
      confirmed: 'Confirmado',
      cancelled: 'Cancelado',
      expired: 'Expirado',
      refund: 'Estorno necessário',
      draft: 'Rascunho',
      published: 'Publicada',
      closed: 'Encerrada',
    }[this.status];
  }
}
