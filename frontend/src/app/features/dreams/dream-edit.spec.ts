import {provideHttpClient} from '@angular/common/http';
import {TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {of} from 'rxjs';

import {DreamEdit} from './dream-edit';
import {DreamsService} from '../../core/services/dreams.service';

describe('DreamEdit', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DreamEdit],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {provide: DreamsService, useValue: {create: () => of(null)}},
      ],
    }).compileComponents();
  });

  it('should create form with controls', () => {
    const fixture = TestBed.createComponent(DreamEdit);
    const component = fixture.componentInstance;
    expect(component.form.contains('content')).toBe(true);
    expect(component.form.contains('date')).toBe(true);
    expect(component.form.contains('title')).toBe(false); // Title removed - auto-generated on backend
  });
});
