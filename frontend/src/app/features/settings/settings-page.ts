import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';

@Component({
  selector: 'app-settings-page',
  imports: [MatSlideToggleModule, MatCardModule],
  templateUrl: './settings-page.html',
  styleUrls: ['./settings-page.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SettingsPage {
  toggleTheme(checked: boolean) {
    document.documentElement.classList.toggle('dark', checked);
  }
}
