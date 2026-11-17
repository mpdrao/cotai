import { Component, inject, input, output, signal } from '@angular/core';
import { errorMessage } from '../../core/copy/ui-copy';
import { Event } from '../../core/models/event.model';
import { EventCatalogService } from '../../core/services/event-catalog.service';
import { CotaiButtonComponent } from '../../shared/cotai-ui';

@Component({
  selector: 'app-event-picker',
  imports: [CotaiButtonComponent],
  templateUrl: './event-picker.component.html',
  styleUrl: './event-picker.component.scss',
})
export class EventPickerComponent {
  private readonly catalog = inject(EventCatalogService);

  /** Code of the event already chosen, if any. */
  readonly selected = input<string | null | undefined>(null);
  /** Validation message from the parent form. */
  readonly error = input<string | undefined>();
  readonly picked = output<Event>();

  protected readonly events = signal<Event[]>([]);
  protected readonly loading = signal(true);
  protected readonly loadError = signal('');

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.loadError.set('');
    this.catalog.events().subscribe({
      next: (events) => {
        this.events.set(events);
        this.loading.set(false);
      },
      error: (e) => {
        this.loadError.set(errorMessage(e));
        this.loading.set(false);
      },
    });
  }
}
