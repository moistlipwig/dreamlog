import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSliderModule } from '@angular/material/slider';
import { Router, RouterLink } from '@angular/router';

import { Mood, CreateDreamRequest } from '../../core/models/dream';
import { DreamsService } from '../../core/services/dreams.service';

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
    RouterLink,
  ],
  templateUrl: './dream-edit.html',
  styleUrls: ['./dream-edit.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DreamEdit {
  private readonly fb = inject(FormBuilder);
  private readonly dreamsService = inject(DreamsService);
  private readonly router = inject(Router);

  // Expose Mood enum to template
  readonly Mood = Mood;
  readonly moodOptions = [
    { value: Mood.POSITIVE, label: 'Positive' },
    { value: Mood.NEUTRAL, label: 'Neutral' },
    { value: Mood.NEGATIVE, label: 'Negative' },
    { value: Mood.NIGHTMARE, label: 'Nightmare' },
    { value: Mood.MIXED, label: 'Mixed' },
  ];

  // State signals
  isSaving = signal(false);
  error = signal<string | null>(null);

  form = this.fb.nonNullable.group({
    title: ['', Validators.required], // eslint-disable-line @typescript-eslint/unbound-method
    content: ['', Validators.required], // eslint-disable-line @typescript-eslint/unbound-method
    date: [new Date(), Validators.required], // eslint-disable-line @typescript-eslint/unbound-method
    tags: [''],
    moodInDream: [null as Mood | null],
    moodAfterDream: [null as Mood | null],
    vividness: [5], // 0-10 scale
    lucid: [false],
  });

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);
    this.error.set(null);

    const value = this.form.getRawValue();

    // Parse tags from comma-separated string
    const tags = value.tags
      ? value.tags
          .split(',')
          .map((t) => t.trim())
          .filter(Boolean)
      : [];

    // Format date as YYYY-MM-DD for LocalDate
    const dateStr = this.formatDateToLocalDate(value.date);

    // Build request matching CreateDreamRequest interface
    const request: CreateDreamRequest = {
      title: value.title,
      content: value.content,
      date: dateStr,
      tags,
      moodInDream: value.moodInDream || undefined,
      moodAfterDream: value.moodAfterDream || undefined,
      vividness: value.vividness,
      lucid: value.lucid,
    };

    this.dreamsService.create(request).subscribe({
      next: (dream) => {
        this.form.markAsPristine();
        this.isSaving.set(false);
        // Navigate to the created dream detail page
        void this.router.navigate(['/app/dreams', dream.id]);
      },
      error: (err) => {
        console.error('Failed to create dream:', err);
        this.error.set('Failed to save dream. Please try again.');
        this.isSaving.set(false);
      },
    });
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
