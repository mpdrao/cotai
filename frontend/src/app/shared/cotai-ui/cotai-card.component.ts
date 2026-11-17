import { Component, Input, booleanAttribute } from '@angular/core';

@Component({
  selector: 'cotai-card',
  standalone: true,
  template: `<section class="cotai-card" [class.cotai-card--tinted]="tinted" [class.cotai-card--raised]="elevation === 'raised'" [class.cotai-card--soft]="elevation === 'soft'"><ng-content></ng-content></section>`,
  styleUrls: ['./cotai-card.component.scss'],
})
export class CotaiCardComponent {
  @Input() elevation: 'flat' | 'soft' | 'raised' = 'flat';
  @Input({ transform: booleanAttribute }) tinted = false;
}
