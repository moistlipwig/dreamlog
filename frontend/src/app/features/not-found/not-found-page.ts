import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found-page',
  imports: [RouterLink, MatCardModule, MatButtonModule],
  templateUrl: './not-found-page.html',
  styleUrls: ['./not-found-page.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotFoundPage {}
