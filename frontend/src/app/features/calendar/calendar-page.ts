import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatDatepickerModule } from '@angular/material/datepicker';

@Component({
  selector: 'app-calendar-page',
  imports: [MatDatepickerModule, MatCardModule],
  templateUrl: './calendar-page.html',
  styleUrls: ['./calendar-page.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CalendarPage {}
