import { Component, computed, input, linkedSignal, output, signal } from '@angular/core';
import { shrinkPhoto } from './shrink-photo';

const TYPES = ['image/jpeg', 'image/png', 'image/webp'];
/** Same limit as the API (PhotoFormat.MAX_BYTES), checked after the photo is shrunk. */
const MAX_BYTES = 5 * 1024 * 1024;

/** Thumbnail of an item photo with "Adicionar / Trocar / Remover foto". Shrinks the picked file; the parent decides when to upload. */
@Component({
  selector: 'app-item-photo',
  template: `
    <div class="photo">
      <div class="frame">
        @if (src() && src() !== brokenSrc()) {
          <img [src]="src()" alt="" (error)="brokenSrc.set(src())" />
        } @else {
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round"
               stroke-linejoin="round" aria-hidden="true">
            <path d="M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3Z" />
            <circle cx="12" cy="13" r="3" />
          </svg>
        }
      </div>
      @if (editable()) {
        <div class="controls">
          <label class="pick" [class.pick--busy]="working()">
            <input class="cotai-visually-hidden" type="file" [accept]="accept" [disabled]="working()" (change)="pick($event)" />
            {{ preparing() ? 'Preparando…' : busy() ? 'Enviando…' : src() ? 'Trocar foto' : 'Adicionar foto' }}
          </label>
          @if (src() && !working()) {
            <button type="button" class="remove" (click)="removed.emit()">Remover foto</button>
          }
        </div>
      }
      @if (problem() || error()) {
        <p class="cotai-field-error" role="alert">{{ problem() || error() }}</p>
      }
    </div>
  `,
  styles: `
    .photo { display: grid; grid-template-columns: auto minmax(0, 1fr); align-items: center; gap: 0.5rem 1rem; }
    .frame {
      display: grid; place-items: center; width: 5rem; aspect-ratio: 1; overflow: hidden;
      border: 1px solid var(--cotai-border); border-radius: var(--cotai-radius-sm);
      background: var(--cotai-surface-peach); color: var(--cotai-terracotta);
    }
    .frame img { width: 100%; height: 100%; object-fit: cover; }
    .frame svg { width: 1.75rem; height: 1.75rem; }
    .controls { display: flex; flex-wrap: wrap; gap: 0.25rem 0.5rem; }
    .pick, .remove {
      display: inline-flex; align-items: center; min-height: var(--cotai-tap-target); padding: 0 1rem;
      border-radius: var(--cotai-radius-pill); font-weight: 600; font-size: 0.875rem; cursor: pointer;
      transition: background var(--cotai-motion);
    }
    .pick { border: 2px solid var(--cotai-primary); background: var(--cotai-surface); color: var(--cotai-primary); }
    .pick:hover { background: var(--cotai-surface-sage); }
    .pick:has(input:focus-visible) { outline: 3px solid var(--cotai-primary); outline-offset: 3px; }
    .pick--busy { opacity: 0.58; cursor: progress; }
    .remove { border: 0; background: transparent; color: var(--cotai-primary); }
    .remove:hover { background: var(--cotai-surface-sage); }
    .cotai-field-error { grid-column: 1 / -1; }
  `,
})
export class ItemPhotoComponent {
  readonly src = input<string | null | undefined>(null);
  readonly editable = input(true);
  readonly busy = input(false);
  /** Error from the parent's upload (e.g. API rejection). */
  readonly error = input('');
  readonly picked = output<File>();
  readonly removed = output<void>();

  protected readonly accept = TYPES.join(',');
  /** Local validation message; cleared when the photo changes (picked, uploaded or removed). */
  protected readonly problem = linkedSignal({ source: this.src, computation: () => '' });
  protected readonly preparing = signal(false);
  protected readonly working = computed(() => this.busy() || this.preparing());
  protected readonly brokenSrc = signal<string | null | undefined>(null);

  protected async pick(event: Event): Promise<void> {
    const field = event.target as HTMLInputElement;
    const picked = field.files?.[0];
    field.value = '';
    if (!picked) return;
    if (!TYPES.includes(picked.type)) {
      this.problem.set('Use uma foto JPG, PNG ou WebP.');
      return;
    }
    this.preparing.set(true);
    const file = await shrinkPhoto(picked);
    this.preparing.set(false);
    if (file.size > MAX_BYTES) {
      this.problem.set('A foto pode ter até 5 MB.');
    } else {
      this.problem.set('');
      this.picked.emit(file);
    }
  }
}
