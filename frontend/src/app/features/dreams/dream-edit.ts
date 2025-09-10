import { ChangeDetectionStrategy, Component } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule, MatNativeDateModule } from '@angular/material/datepicker';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
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
    MatButtonModule
  ],
  templateUrl: './dream-edit.html',
  styleUrl: './dream-edit.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DreamEdit {
  private fb = inject(FormBuilder);
  private dreams = inject(DreamsService);

  form = this.fb.nonNullable.group({
    title: ['', Validators.required],
    content: ['', Validators.required],
    date: [new Date(), Validators.required],
    tags: [''],
    mood: [3, Validators.required]
  });

  save() {
    if (this.form.valid) {
      this.dreams.create(this.form.getRawValue()).subscribe(() => {
        this.form.markAsPristine();
      });
    }
  }

  hasPendingChanges() {
    return this.form.dirty;
  }
}
