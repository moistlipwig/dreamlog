import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

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
  ],
  templateUrl: './dream-edit.html',
  styleUrls: ['./dream-edit.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DreamEdit {
  private fb = inject(FormBuilder);
  private dreams = inject(DreamsService);

  form = this.fb.nonNullable.group({
    title: ['', Validators.required], // eslint-disable-line @typescript-eslint/unbound-method
    content: ['', Validators.required], // eslint-disable-line @typescript-eslint/unbound-method
    date: [new Date(), Validators.required], // eslint-disable-line @typescript-eslint/unbound-method
    tags: [''],
    mood: [3, Validators.required], // eslint-disable-line @typescript-eslint/unbound-method
  });

  save() {
    if (this.form.valid) {
      const value = this.form.getRawValue();
      const tags = value.tags
        ? value.tags
            .split(',')
            .map((t) => t.trim())
            .filter(Boolean)
        : [];
      this.dreams.create({ ...value, date: value.date.toISOString(), tags }).subscribe(() => {
        this.form.markAsPristine();
      });
    }
  }

  hasPendingChanges() {
    return this.form.dirty;
  }
}
