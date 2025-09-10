import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { DreamEdit } from './dream-edit';
import { provideHttpClient } from '@angular/common/http';
import { DreamsService } from '../../core/services/dreams.service';

describe('DreamEdit', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DreamEdit],
      providers: [
        provideHttpClient(),
        { provide: DreamsService, useValue: { create: () => of(null) } }
      ]
    }).compileComponents();
  });

  it('should create form with controls', () => {
    const fixture = TestBed.createComponent(DreamEdit);
    const component = fixture.componentInstance;
    expect(component.form.contains('title')).toBeTrue();
  });
});
