import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';

import { PodcastApi } from '../../core/api/podcast.api';
import { PodcastHAL, PodcastUpdateHAL } from '../../core/models/podcast.model';

interface DraftState {
  title: string;
  url: string;
  hasToBeDeleted: boolean;
  tagsCsv: string;
}

@Component({
  selector: 'ps-podcast-edit-dialog',
  standalone: true,
  imports: [
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule,
  ],
  template: `
    <h2 mat-dialog-title>Edit podcast</h2>
    <mat-dialog-content class="dialog">
      <mat-form-field appearance="outline" subscriptSizing="dynamic">
        <mat-label>Title</mat-label>
        <input
          matInput
          name="title"
          required
          [ngModel]="draft().title"
          (ngModelChange)="patch({ title: $event })"
        />
      </mat-form-field>

      <mat-form-field appearance="outline" subscriptSizing="dynamic">
        <mat-label>Source URL</mat-label>
        <input
          matInput
          name="url"
          type="url"
          [ngModel]="draft().url"
          (ngModelChange)="patch({ url: $event })"
        />
        <mat-hint>RSS feed, channel page, etc. Empty for upload-only podcasts.</mat-hint>
      </mat-form-field>

      <mat-form-field appearance="outline" subscriptSizing="dynamic">
        <mat-label>Tags</mat-label>
        <input
          matInput
          name="tags"
          [ngModel]="draft().tagsCsv"
          (ngModelChange)="patch({ tagsCsv: $event })"
        />
        <mat-hint>Comma-separated. Tag autocomplete lands in PR 7.</mat-hint>
      </mat-form-field>

      <mat-slide-toggle
        [checked]="draft().hasToBeDeleted"
        (change)="patch({ hasToBeDeleted: $event.checked })"
      >
        Auto-delete old downloaded episodes
      </mat-slide-toggle>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [disabled]="saving()" (click)="onCancel()">Cancel</button>
      <button
        mat-flat-button
        color="primary"
        [disabled]="saving() || !draft().title.trim()"
        (click)="onSave()"
      >
        <mat-icon>save</mat-icon>
        Save
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .dialog {
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
      min-width: min(28rem, 90vw);
      padding-top: 1rem;
    }
    .dialog mat-slide-toggle {
      margin-top: 0.5rem;
    }
    .dialog__error {
      color: var(--mat-sys-error);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PodcastEditDialogComponent {
  private readonly api = inject(PodcastApi);
  private readonly snackbar = inject(MatSnackBar);
  private readonly dialogRef = inject(MatDialogRef<PodcastEditDialogComponent, boolean>);
  private readonly podcast = inject<PodcastHAL>(MAT_DIALOG_DATA);

  protected readonly draft = signal<DraftState>({
    title: this.podcast.title,
    url: this.podcast.url ?? '',
    hasToBeDeleted: this.podcast.hasToBeDeleted,
    tagsCsv: this.podcast.tags.map((t) => t.name).join(', '),
  });
  protected readonly saving = signal(false);

  protected patch(change: Partial<DraftState>) {
    this.draft.update((d) => ({ ...d, ...change }));
  }

  protected onCancel() {
    this.dialogRef.close(false);
  }

  protected onSave() {
    const d = this.draft();
    const existingTagsByName = new Map(
      this.podcast.tags.map((t) => [t.name.trim().toLowerCase(), t.id]),
    );
    const body: PodcastUpdateHAL = {
      id: this.podcast.id,
      title: d.title.trim(),
      url: d.url.trim() || null,
      hasToBeDeleted: d.hasToBeDeleted,
      tags: d.tagsCsv
        .split(',')
        .map((t) => t.trim())
        .filter((t) => t.length > 0)
        .map((name) => {
          const existing = existingTagsByName.get(name.toLowerCase());
          return existing ? { id: existing, name } : { name };
        }),
      cover: {
        width: this.podcast.cover.width,
        height: this.podcast.cover.height,
        url: this.podcast.cover.url,
      },
    };
    this.saving.set(true);
    this.api.update(this.podcast.id, body).subscribe({
      next: () => {
        this.saving.set(false);
        this.snackbar.open('Podcast saved', undefined, { duration: 2500 });
        this.dialogRef.close(true);
      },
      error: () => {
        this.saving.set(false);
        this.snackbar.open('Could not save the podcast', 'Dismiss', { duration: 4000 });
      },
    });
  }
}
