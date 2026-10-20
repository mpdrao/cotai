import { Component, Input } from '@angular/core';

@Component({
  selector: 'cotai-progress',
  standalone: true,
  template: `
    <div class="cotai-track"
         role="progressbar"
         [attr.aria-label]="label"
         aria-valuemin="0"
         aria-valuemax="100"
         [attr.aria-valuenow]="safeValue">
      <span class="cotai-fill" [style.width.%]="safeValue"></span>
    </div>
  `,
  styleUrls: ['./cotai-progress.component.scss'],
})
export class CotaiProgressComponent {
  @Input() value = 0;
  @Input() label = 'Progresso das cotas';
  get safeValue(): number {
    return Number.isFinite(this.value) ? Math.min(100, Math.max(0, this.value)) : 0;
  }
}
