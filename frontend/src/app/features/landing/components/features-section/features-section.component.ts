import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';

interface Feature {
  icon: string;
  title: string;
  description: string;
}

@Component({
  selector: 'app-features-section',
  imports: [MatCardModule, MatIconModule],
  templateUrl: './features-section.component.html',
  styleUrls: ['./features-section.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FeaturesSectionComponent {
  features: Feature[] = [
    {
      icon: 'edit_note',
      title: 'Dream Journaling',
      description:
        'Capture your dreams with a beautiful, intuitive interface. Add tags, emotions, and detailed notes.',
    },
    {
      icon: 'calendar_month',
      title: 'Calendar View',
      description:
        'Visualize your dream patterns over time. Track frequency and identify recurring themes.',
    },
    {
      icon: 'search',
      title: 'Smart Search',
      description: 'Find specific dreams instantly. Search by keywords, tags, dates, or emotions.',
    },
    {
      icon: 'insights',
      title: 'AI Insights',
      description: 'Get personalized insights about your dreams. Discover patterns and meanings.',
    },
  ];
}
