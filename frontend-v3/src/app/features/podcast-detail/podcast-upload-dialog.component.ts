import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { PodcastApi } from '../../core/api/podcast.api';

@Component({
  selector: 'ps-podcast-upload-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  template: `
    <h2 mat-dialog-title>Upload episode</h2>
    <mat-dialog-content class="dialog">
      <p class="dialog__hint">
        Drop or pick an audio/video file to add it as a new episode.
      </p>

      <input
        #fileInput
        type="file"
        class="dialog__input"
        accept="audio/*,video/*"
        (change)="onFileSelected($event)"
      />

      <div class="dialog__row">
        <button
          mat-stroked-button
          type="button"
          (click)="fileInput.click()"
          [disabled]="uploading()"
        >
          <mat-icon>attach_file</mat-icon>
          Choose file
        </button>

        @if (file(); as f) {
          <span class="dialog__file" [title]="f.name">{{ f.name }}</span>
          <button
            mat-icon-button
            type="button"
            aria-label="Clear selection"
            (click)="onClear()"
            [disabled]="uploading()"
          >
            <mat-icon>close</mat-icon>
          </button>
        }
      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [disabled]="uploading()" (click)="onCancel()">Cancel</button>
      <button
        mat-flat-button
        color="primary"
        [disabled]="!file() || uploading()"
        (click)="onUpload()"
      >
        @if (uploading()) {
          <mat-spinner diameter="18" />
        } @else {
          <mat-icon>upload</mat-icon>
        }
        Upload
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .dialog {
      display: flex;
      flex-direction: column;
      gap: 1rem;
      min-width: min(28rem, 90vw);
    }
    .dialog__hint {
      font: var(--mat-sys-body-medium);
      color: var(--mat-sys-on-surface-variant);
      margin: 0;
    }
    .dialog__input {
      display: none;
    }
    .dialog__row {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }
    .dialog__file {
      flex: 1 1 auto;
      min-width: 0;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      font: var(--mat-sys-body-medium);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PodcastUploadDialogComponent {
  private readonly api = inject(PodcastApi);
  private readonly snackbar = inject(MatSnackBar);
  private readonly dialogRef = inject(
    MatDialogRef<PodcastUploadDialogComponent, boolean>,
  );
  private readonly podcastId = inject<string>(MAT_DIALOG_DATA);

  protected readonly file = signal<File | undefined>(undefined);
  protected readonly uploading = signal(false);

  protected onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    this.file.set(input.files?.[0]);
  }

  protected onClear() {
    this.file.set(undefined);
  }

  protected onCancel() {
    this.dialogRef.close(false);
  }

  protected onUpload() {
    const f = this.file();
    if (!f) return;
    this.uploading.set(true);
    this.api.upload(this.podcastId, f).subscribe({
      next: () => {
        this.uploading.set(false);
        this.snackbar.open('Episode uploaded', undefined, { duration: 2500 });
        this.dialogRef.close(true);
      },
      error: () => {
        this.uploading.set(false);
        this.snackbar.open('Upload failed', 'Dismiss', { duration: 4000 });
      },
    });
  }
}
