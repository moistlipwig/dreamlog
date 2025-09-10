import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatCalendarModule } from '@angular/material/datepicker';

@Component({
  selector: 'app-calendar-page',
  imports: [MatCalendarModule],
  templateUrl: './calendar-page.html',
  styleUrl: './calendar-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CalendarPage {}
