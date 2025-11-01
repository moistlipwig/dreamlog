import {ChangeDetectionStrategy, Component, inject, OnInit, signal} from '@angular/core';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatNativeDateModule} from '@angular/material/core';
import {MatDatepickerModule} from '@angular/material/datepicker';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatSelectModule} from '@angular/material/select';
import {MatSliderModule} from '@angular/material/slider';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';

import {CreateDreamRequest, Dream, Mood} from '../../core/models/dream';
import {DreamsService} from '../../core/services/dreams.service';

@Component({
  selector: 'app-dream-edit',
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatSelectModule,
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatSliderModule,
    MatProgressSpinnerModule,
    MatIconModule,
    RouterLink,
  ],
  templateUrl: './dream-edit.html',
  styleUrls: ['./dream-edit.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DreamEdit implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly dreamsService = inject(DreamsService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  // Expose Mood enum to template
  readonly Mood = Mood;
  readonly moodOptions = [
    {value: Mood.POSITIVE, label: '😊 Positive', emoji: '😊'},
    {value: Mood.NEUTRAL, label: '😐 Neutral', emoji: '😐'},
    {value: Mood.NEGATIVE, label: '😢 Negative', emoji: '😢'},
    {value: Mood.NIGHTMARE, label: '😱 Nightmare', emoji: '😱'},
    {value: Mood.MIXED, label: '🤔 Mixed', emoji: '🤔'},
  ];

  // State signals
  isSaving = signal(false);
  error = signal<string | null>(null);
  private editingDream: Dream | null = null;
  isEditMode = false;

  form = this.fb.nonNullable.group({
    title: ['', [Validators.maxLength(120)]],
    content: ['', Validators.required], // eslint-disable-line @typescript-eslint/unbound-method
    date: [new Date(), Validators.required], // eslint-disable-line @typescript-eslint/unbound-method
    tags: [''],
    moodInDream: [null as Mood | null],
    moodAfterDream: [null as Mood | null],
    vividness: [5], // 0-10 scale
    lucid: [false],
  });

  ngOnInit(): void {
    // Check if we're editing an existing dream
    const dream = this.route.snapshot.data['dream'] as Dream | undefined;
    if (dream) {
      this.editingDream = dream;
      this.isEditMode = true;
      this.populateForm(dream);
    }
  }

  private populateForm(dream: Dream): void {
    // Parse tags array to comma-separated string
    const tagsString = dream.tags.join(', ');

    // Parse date string (YYYY-MM-DD) to Date object
    const dateObj = new Date(dream.date);

    this.form.patchValue({
      title: dream.title,
      content: dream.content,
      date: dateObj,
      tags: tagsString,
      moodInDream: dream.moodInDream,
      moodAfterDream: dream.moodAfterDream,
      vividness: dream.vividness,
      lucid: dream.lucid,
    });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);
    this.error.set(null);

    const value = this.form.getRawValue();
    const trimmedTitle = value.title?.trim();

    // Parse tags from comma-separated string
    const tags = value.tags
      ? value.tags
        .split(',')
        .map((t) => t.trim())
        .filter(Boolean)
      : [];

    // Format date as YYYY-MM-DD for LocalDate
    const dateStr = this.formatDateToLocalDate(value.date);

    // Build request matching CreateDreamRequest interface (UpdateDreamRequest is identical)
    const request: CreateDreamRequest = {
      title: trimmedTitle || undefined,
      content: value.content,
      date: dateStr,
      tags,
      moodInDream: value.moodInDream || undefined,
      moodAfterDream: value.moodAfterDream || undefined,
      vividness: value.vividness,
      lucid: value.lucid,
    };

    if (this.editingDream) {
      this.dreamsService.update(this.editingDream.id, request).subscribe({
        next: () => {
          this.form.markAsPristine();
          this.isSaving.set(false);
          void this.router.navigate(['/app/dreams', this.editingDream!.id]);
        },
        error: (err) => {
          console.error('Failed to update dream:', err);
          this.error.set('Failed to save dream. Please try again.');
          this.isSaving.set(false);
        },
      });
    } else {
      this.dreamsService.create(request).subscribe({
        next: (response) => {
          this.form.markAsPristine();
          this.isSaving.set(false);
          void this.router.navigate(['/app/dreams', response.id]);
        },
        error: (err) => {
          console.error('Failed to create dream:', err);
          this.error.set('Failed to save dream. Please try again.');
          this.isSaving.set(false);
        },
      });
    }
  }

  /**
   * Format Date to YYYY-MM-DD for backend LocalDate.
   */
  private formatDateToLocalDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  hasPendingChanges(): boolean {
    return this.form.dirty;
  }
}
