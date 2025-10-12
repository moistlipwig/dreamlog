import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-footer-section',
  imports: [],
  templateUrl: './footer-section.component.html',
  styleUrls: ['./footer-section.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FooterSectionComponent {
  currentYear = new Date().getFullYear();
}
