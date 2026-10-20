import { Component, EventEmitter, Input, Output, booleanAttribute } from '@angular/core';

/** Primary button for the Cota Ae! design system. */
@Component({
  selector: 'cotai-button',
  standalone: true,
  template: `
    <button
      class="cotai-button"
      [class.cotai-button--secondary]="variant === 'secondary'"
      [class.cotai-button--ghost]="variant === 'ghost'"
      [class.cotai-button--full]="fullWidth"
      [attr.type]="type"
      [disabled]="disabled || loading"
      [attr.aria-busy]="loading ? 'true' : null"
      (click)="pressed.emit($event)">
      <ng-content></ng-content>
    </button>
  `,
  styleUrls: ['./cotai-button.component.scss'],
})
export class CotaiButtonComponent {
  @Input() variant: 'primary' | 'secondary' | 'ghost' = 'primary';
  @Input() type: 'button' | 'submit' | 'reset' = 'button';
  @Input({ transform: booleanAttribute }) disabled = false;
  @Input({ transform: booleanAttribute }) loading = false;
  @Input({ transform: booleanAttribute }) fullWidth = false;
  @Output() pressed = new EventEmitter<MouseEvent>();
}
