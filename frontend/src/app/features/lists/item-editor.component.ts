import { Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ItemDraft, ItemErrors } from './item-draft';

/** Fields of one item (name, unit, price per quota, quotas). The parent owns the draft and validates it. */
@Component({
  selector: 'app-item-editor',
  imports: [FormsModule],
  template: `
    <div class="fields">
      <label class="cotai-field name">
        <span class="cotai-label">Item</span>
        <input class="cotai-input" [id]="prefix() + '-name'" maxlength="80" [(ngModel)]="draft().name"
               [ngModelOptions]="{ standalone: true }" (ngModelChange)="changed.emit()" [attr.aria-invalid]="!!errors().name" />
        @if (errors().name) { <span class="cotai-field-error">{{ errors().name }}</span> }
      </label>
      <label class="cotai-field">
        <span class="cotai-label">Unidade</span>
        <input class="cotai-input" [id]="prefix() + '-unit'" maxlength="30" placeholder="kg, caixa, pacote"
               [(ngModel)]="draft().unit" [ngModelOptions]="{ standalone: true }" (ngModelChange)="changed.emit()" [attr.aria-invalid]="!!errors().unit" />
        @if (errors().unit) { <span class="cotai-field-error">{{ errors().unit }}</span> }
      </label>
      <label class="cotai-field">
        <span class="cotai-label">Valor da cota (R$)</span>
        <input class="cotai-input" [id]="prefix() + '-price'" inputmode="decimal" placeholder="90,00"
               [(ngModel)]="draft().price" [ngModelOptions]="{ standalone: true }" (ngModelChange)="changed.emit()" [attr.aria-invalid]="!!errors().price" />
        @if (errors().price) { <span class="cotai-field-error">{{ errors().price }}</span> }
      </label>
      <label class="cotai-field">
        <span class="cotai-label">Cotas</span>
        <input class="cotai-input" [id]="prefix() + '-quotas'" type="number" inputmode="numeric" min="1" max="1000"
               [(ngModel)]="draft().totalQuotas" [ngModelOptions]="{ standalone: true }" (ngModelChange)="changed.emit()"
               [attr.aria-invalid]="!!errors().totalQuotas" />
        @if (errors().totalQuotas) { <span class="cotai-field-error">{{ errors().totalQuotas }}</span> }
      </label>
    </div>
  `,
  styles: `
    .fields { display: grid; grid-template-columns: minmax(0, 1fr); gap: 1.5rem; }
    .name { grid-column: 1 / -1; }
    .fields > label:nth-child(2) { grid-column: 1 / -1; }
    @media (min-width: 40rem) {
      .fields { grid-template-columns: repeat(2, minmax(0, 1fr)); }
    }
    @media (min-width: 64rem) {
      .cotai-label { display: flex; align-items: end; min-height: 2.75rem; }
      .fields { grid-template-columns: minmax(0, 2fr) minmax(0, 1.2fr) minmax(0, 1fr) minmax(0, 0.8fr); align-items: start; }
      .name, .fields > label:nth-child(2) { grid-column: auto; }
    }
  `,
})
export class ItemEditorComponent {
  readonly draft = input.required<ItemDraft>();
  readonly errors = input<ItemErrors>({});
  /** Unique id prefix for the inputs (e.g. "item-3"). */
  readonly prefix = input.required<string>();
  /** Emitted after any field changes, so the parent can recompute totals. */
  readonly changed = output<void>();
}
