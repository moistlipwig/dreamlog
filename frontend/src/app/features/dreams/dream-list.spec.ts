import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { DreamList } from './dream-list';
import { DreamsService } from '../../core/services/dreams.service';

describe('DreamList', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DreamList],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: DreamsService,
          useValue: {
            list: () =>
              of({
                content: [
                  {
                    id: '1',
                    title: 'A',
                    content: 'c',
                    date: '2020-01-01',
                    tags: [],
                    moodInDream: null,
                    moodAfterDream: null,
                    vividness: 5,
                    lucid: false,
                  },
                ],
                totalElements: 1,
                totalPages: 1,
                size: 10,
                number: 0,
                first: true,
                last: true,
                empty: false,
              }),
          },
        },
      ],
    }).compileComponents();
  });

  it('renders list', async () => {
    const fixture = TestBed.createComponent(DreamList);
    fixture.detectChanges();
    await fixture.whenStable(); // Wait for async pipe to resolve
    fixture.detectChanges(); // Re-render after data loads
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelectorAll('mat-card').length).toBe(1);
  });
});
