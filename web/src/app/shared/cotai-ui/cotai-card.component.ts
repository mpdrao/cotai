import { Component, Input, booleanAttribute } from '@angular/core';

@Component({
  selector: 'cotai-card',
  standalone: true,
  template: `<section class="cotai-card" [class.cotai-card--tinted]="tinted"><ng-content></ng-content></section>`,
  styleUrls: ['./cotai-card.component.scss'],
})
export class CotaiCardComponent {
  @Input({ transform: booleanAttribute }) tinted = false;
}
