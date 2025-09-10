import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterOutlet } from '@angular/router';

import { SearchBar } from './search-bar';

@Component({
  selector: 'app-app-shell',
  imports: [RouterOutlet, MatToolbarModule, MatButtonModule, MatIconModule, MatSnackBarModule, SearchBar],
  templateUrl: './app-shell.html',
  styleUrls: ['./app-shell.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppShell {}
