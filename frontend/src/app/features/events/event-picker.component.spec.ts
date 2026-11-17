import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { Event } from '../../core/models/event.model';
import { EventCatalogService } from '../../core/services/event-catalog.service';
import { EventPickerComponent } from './event-picker.component';

const CATALOG: Event[] = [
  { code: 'BIRTHDAY', name: 'Aniversário', defaultReservationTtlMinutes: 30 },
  { code: 'WEDDING', name: 'Casamento', defaultReservationTtlMinutes: 30 },
];

function render(events: () => Observable<Event[]>) {
  TestBed.configureTestingModule({
    imports: [EventPickerComponent],
    providers: [{ provide: EventCatalogService, useValue: { events } }],
  });
  const fixture = TestBed.createComponent(EventPickerComponent);
  fixture.detectChanges();
  return fixture;
}

describe('EventPickerComponent', () => {
  it('shows one option per event of the catalog', () => {
    const page = render(() => of(CATALOG)).nativeElement as HTMLElement;

    const labels = Array.from(page.querySelectorAll('.event-option span')).map((el) => el.textContent?.trim());
    expect(labels).toEqual(['Aniversário', 'Casamento']);
  });

  it('tells the parent which event was picked', () => {
    const fixture = render(() => of(CATALOG));
    const picked: Event[] = [];
    fixture.componentInstance.picked.subscribe((event) => picked.push(event));

    (fixture.nativeElement as HTMLElement).querySelector<HTMLInputElement>('#event-WEDDING')!.dispatchEvent(new globalThis.Event('change'));

    expect(picked.map((t) => t.code)).toEqual(['WEDDING']);
  });

  it('marks the event chosen by the parent', () => {
    const fixture = render(() => of(CATALOG));
    fixture.componentRef.setInput('selected', 'BIRTHDAY');
    fixture.detectChanges();

    const selected = (fixture.nativeElement as HTMLElement).querySelector('.event-option--selected');
    expect(selected?.textContent?.trim()).toBe('Aniversário');
  });

  it('offers a retry when the catalog cannot be loaded', () => {
    let calls = 0;
    const fixture = render(() => {
      calls++;
      return calls === 1 ? throwError(() => new HttpErrorResponse({ status: 503 })) : of(CATALOG);
    });
    const page = fixture.nativeElement as HTMLElement;
    expect(page.querySelector('[role="alert"]')).not.toBeNull();

    page.querySelector<HTMLButtonElement>('.load-error button')!.click();
    fixture.detectChanges();

    expect(page.querySelector('[role="alert"]')).toBeNull();
    expect(page.querySelectorAll('.event-option')).toHaveLength(2);
  });
});
