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
              of([{ id: '1', title: 'A', content: 'c', date: '2020-01-01', tags: [], mood: 3 }]),
          },
        },
      ],
    }).compileComponents();
  });

  it('renders list', () => {
    const fixture = TestBed.createComponent(DreamList);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelectorAll('mat-card').length).toBe(1);
  });
});
