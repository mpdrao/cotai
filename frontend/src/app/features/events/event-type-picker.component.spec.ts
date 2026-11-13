import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { EventType } from '../../core/models/event-type.model';
import { EventCatalogService } from '../../core/services/event-catalog.service';
import { EventTypePickerComponent } from './event-type-picker.component';

const CATALOG: EventType[] = [
  { code: 'BIRTHDAY', name: 'Aniversário', defaultReservationTtlMinutes: 30 },
  { code: 'WEDDING', name: 'Casamento', defaultReservationTtlMinutes: 30 },
];

function render(eventTypes: () => Observable<EventType[]>) {
  TestBed.configureTestingModule({
    imports: [EventTypePickerComponent],
    providers: [{ provide: EventCatalogService, useValue: { eventTypes } }],
  });
  const fixture = TestBed.createComponent(EventTypePickerComponent);
  fixture.detectChanges();
  return fixture;
}

describe('EventTypePickerComponent', () => {
  it('shows one option per event type of the catalog', () => {
    const page = render(() => of(CATALOG)).nativeElement as HTMLElement;

    const labels = Array.from(page.querySelectorAll('.type span')).map((el) => el.textContent?.trim());
    expect(labels).toEqual(['Aniversário', 'Casamento']);
  });

  it('tells the parent which type was picked', () => {
    const fixture = render(() => of(CATALOG));
    const picked: EventType[] = [];
    fixture.componentInstance.picked.subscribe((type) => picked.push(type));

    (fixture.nativeElement as HTMLElement).querySelector<HTMLInputElement>('#type-WEDDING')!.dispatchEvent(new Event('change'));

    expect(picked.map((t) => t.code)).toEqual(['WEDDING']);
  });

  it('marks the type chosen by the parent', () => {
    const fixture = render(() => of(CATALOG));
    fixture.componentRef.setInput('selected', 'BIRTHDAY');
    fixture.detectChanges();

    const selected = (fixture.nativeElement as HTMLElement).querySelector('.type--selected');
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
    expect(page.querySelectorAll('.type')).toHaveLength(2);
  });
});
