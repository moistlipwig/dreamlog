import { ChangeDetectionStrategy, Component } from '@angular/core';

import { FeaturesSectionComponent } from './components/features-section/features-section.component';
import { FooterSectionComponent } from './components/footer-section/footer-section.component';
import { HeroSectionComponent } from './components/hero-section/hero-section.component';

@Component({
  selector: 'app-landing-page',
  imports: [HeroSectionComponent, FeaturesSectionComponent, FooterSectionComponent],
  templateUrl: './landing-page.component.html',
  styleUrls: ['./landing-page.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LandingPageComponent {}
