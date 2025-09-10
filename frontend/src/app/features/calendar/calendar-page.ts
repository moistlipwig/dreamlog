import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatDatepickerModule } from '@angular/material/datepicker';

@Component({
  selector: 'app-calendar-page',
  imports: [MatDatepickerModule],
  templateUrl: './calendar-page.html',
  styleUrls: ['./calendar-page.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CalendarPage {}
