import {provideHttpClient} from '@angular/common/http';
import {TestBed} from '@angular/core/testing';
import {ActivatedRoute, provideRouter, Router} from '@angular/router';
import {of} from 'rxjs';

import {DreamEdit} from './dream-edit';
import {Dream, Mood} from '../../core/models/dream';
import {DreamsService} from '../../core/services/dreams.service';

const mockDream: Dream = {
  id: 'existing-id',
  date: '2025-01-20',
  title: 'Moonlit Chase',
  content: 'Chased by shadows under the moon.',
  moodInDream: Mood.NEGATIVE,
  moodAfterDream: Mood.NEUTRAL,
  vividness: 6,
  lucid: false,
  tags: ['night', 'chase'],
};

type DreamsServiceMock = Pick<DreamsService, 'create' | 'update'>;
type RouterMock = Pick<Router, 'navigate'>;

describe('DreamEdit', () => {
  let dreamsService: jest.Mocked<DreamsServiceMock>;
  let router: jest.Mocked<RouterMock>;
  let routeStub: ActivatedRoute;

  beforeEach(async () => {
    dreamsService = {
      create: jest.fn().mockReturnValue(of(mockDream)),
      update: jest.fn().mockReturnValue(of(mockDream)),
    };

    router = {
      navigate: jest.fn().mockResolvedValue(true),
    };

    await TestBed.configureTestingModule({
      imports: [DreamEdit],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {provide: DreamsService, useValue: dreamsService},
        {provide: Router, useValue: router},
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              data: {},
            },
          },
        },
      ],
    }).compileComponents();

    routeStub = TestBed.inject(ActivatedRoute);
  });

  beforeEach(() => {
    dreamsService.create.mockClear();
    dreamsService.update.mockClear();
    router.navigate.mockClear();
    routeStub.snapshot.data = {};
  });

  it('should create form with controls including title', () => {
    const fixture = TestBed.createComponent(DreamEdit);
    const component = fixture.componentInstance;

    expect(component.form.contains('content')).toBe(true);
    expect(component.form.contains('date')).toBe(true);
    expect(component.form.contains('title')).toBe(true);
  });

  it('should populate form and headline when editing an existing dream', () => {
    routeStub.snapshot.data['dream'] = mockDream;
    const fixture = TestBed.createComponent(DreamEdit);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.isEditMode).toBe(true);
    expect(component.form.getRawValue().title).toBe(mockDream.title);

    const host = fixture.nativeElement as HTMLElement;
    const heading = host.querySelector<HTMLHeadingElement>('h1');
    expect(heading).not.toBeNull();
    expect(heading?.textContent ?? '').toContain('Edit Your Dream');
  });

  it('should send trimmed title when saving a new dream', () => {
    const fixture = TestBed.createComponent(DreamEdit);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.form.patchValue({
      title: '  Floating Through Clouds  ',
      content: 'I was floating effortlessly.',
    });

    component.save();

    expect(dreamsService.create).toHaveBeenCalledTimes(1);
    const capturedRequest = dreamsService.create.mock.calls[0][0];
    expect(capturedRequest.title).toBe('Floating Through Clouds');
    expect(router.navigate).toHaveBeenCalledWith(['/app/dreams', mockDream.id]);
  });

  it('should include title in update request', () => {
    routeStub.snapshot.data['dream'] = mockDream;
    const fixture = TestBed.createComponent(DreamEdit);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.form.patchValue({title: 'Rewritten Dream Title'});
    component.save();

    expect(dreamsService.update).toHaveBeenCalledWith(
      mockDream.id,
      expect.objectContaining({
        title: 'Rewritten Dream Title',
      }),
    );
  });
});
