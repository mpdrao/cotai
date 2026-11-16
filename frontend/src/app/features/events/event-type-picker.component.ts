import { Component, inject, input, output, signal } from '@angular/core';
import { errorMessage } from '../../core/copy/ui-copy';
import { EventType } from '../../core/models/event-type.model';
import { EventCatalogService } from '../../core/services/event-catalog.service';
import { CotaiButtonComponent } from '../../shared/cotai-ui';

@Component({
  selector: 'app-event-type-picker',
  imports: [CotaiButtonComponent],
  templateUrl: './event-type-picker.component.html',
  styleUrl: './event-type-picker.component.scss',
})
export class EventTypePickerComponent {
  private readonly catalog = inject(EventCatalogService);

  /** Code of the type already chosen, if any. */
  readonly selected = input<string | null | undefined>(null);
  /** Validation message from the parent form. */
  readonly error = input<string | undefined>();
  readonly picked = output<EventType>();

  protected readonly types = signal<EventType[]>([]);
  protected readonly loading = signal(true);
  protected readonly loadError = signal('');

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.loadError.set('');
    this.catalog.eventTypes().subscribe({
      next: (types) => {
        this.types.set(types);
        this.loading.set(false);
      },
      error: (e) => {
        this.loadError.set(errorMessage(e));
        this.loading.set(false);
      },
    });
  }
}
