import {
  ChangeDetectionStrategy,
  Component,
  effect,
  inject,
  signal,
} from '@angular/core';
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
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { PlaylistApi } from '../../core/api/playlist.api';
import { PlaylistWithItemsHAL } from '../../core/models/playlist.model';

const PREVIEW_DEBOUNCE_MS = 500;

@Component({
  selector: 'ps-playlist-edit-dialog',
  standalone: true,
  imports: [
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <h2 mat-dialog-title>Edit playlist "{{ playlist.name }}"</h2>
    <mat-dialog-content class="dialog">
      <div class="dialog__preview">
        <img class="preview__cover" [src]="previewUrl()" [alt]="playlist.name" />
        <div class="preview__meta">
          <div class="preview__title">{{ playlist.name }}</div>
          <div class="preview__sub">
            {{ playlist.items.length }} episode{{ playlist.items.length === 1 ? '' : 's' }}
          </div>
        </div>
      </div>

      <mat-form-field appearance="outline" subscriptSizing="dynamic">
        <mat-label>Cover URL</mat-label>
        <input
          matInput
          type="url"
          [ngModel]="coverUrlInput()"
          (ngModelChange)="coverUrlInput.set($event)"
          [disabled]="saving()"
        />
        <mat-hint>Paste any image URL.</mat-hint>
      </mat-form-field>

      <p class="dialog__note">
        Renaming a playlist isn't supported yet — only the cover can be
        changed here.
      </p>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button [disabled]="saving()" (click)="onCancel()">Cancel</button>
      <button
        mat-flat-button
        color="primary"
        [disabled]="saving() || !isValid()"
        (click)="onSave()"
      >
        @if (saving()) {
          <mat-spinner diameter="18" />
        } @else {
          <mat-icon>save</mat-icon>
        }
        Save
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .dialog {
      display: flex;
      flex-direction: column;
      gap: 1.25rem;
      min-width: min(32rem, 90vw);
    }
    .dialog > :first-child {
      margin-top: 1.5rem;
    }
    .dialog__preview {
      background: var(--mat-sys-surface-container-low);
      border-radius: 16px;
      padding: 0.75rem 1rem;
      min-height: 80px;
      display: flex;
      align-items: center;
      gap: 1rem;
    }
    .preview__cover {
      width: 64px;
      height: 64px;
      object-fit: cover;
      border-radius: 12px;
      flex: 0 0 auto;
      display: block;
      background: var(--mat-sys-surface-container);
    }
    .preview__meta {
      flex: 1 1 auto;
      min-width: 0;
      display: flex;
      flex-direction: column;
      gap: 2px;
    }
    .preview__title {
      font: var(--mat-sys-title-small);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .preview__sub {
      font: var(--mat-sys-body-small);
      color: var(--mat-sys-on-surface-variant);
    }
    .dialog__note {
      font: var(--mat-sys-body-small);
      color: var(--mat-sys-on-surface-variant);
      margin: 0;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlaylistEditDialogComponent {
  private readonly playlistApi = inject(PlaylistApi);
  private readonly snackbar = inject(MatSnackBar);
  private readonly dialogRef =
    inject(MatDialogRef<PlaylistEditDialogComponent, boolean>);
  protected readonly playlist = inject<PlaylistWithItemsHAL>(MAT_DIALOG_DATA);

  // Current cover served at the same URL across edits; we cache-bust it
  // optimistically as the user pastes so the preview matches the new URL.
  private readonly currentCoverUrl = `/api/v1/playlists/${this.playlist.id}/cover.jpg`;

  protected readonly coverUrlInput = signal<string>('');
  protected readonly previewUrl = signal<string>(this.currentCoverUrl);
  private readonly updateMutation = this.playlistApi.updateCoverMutation();
  protected readonly saving = this.updateMutation.isPending;

  protected readonly isValid = (): boolean => {
    const url = this.coverUrlInput().trim();
    return /^https?:\/\/\S+/.test(url);
  };

  private pendingPreview: ReturnType<typeof setTimeout> | undefined;

  constructor() {
    // Debounced preview swap as the user types. We don't fetch dimensions —
    // the backend re-resolves the cover via /find + ImageService server-side.
    effect(() => {
      const url = this.coverUrlInput().trim();
      if (this.pendingPreview) {
        clearTimeout(this.pendingPreview);
        this.pendingPreview = undefined;
      }
      if (!/^https?:\/\/\S+/.test(url)) {
        this.previewUrl.set(this.currentCoverUrl);
        return;
      }
      this.pendingPreview = setTimeout(
        () => this.previewUrl.set(url),
        PREVIEW_DEBOUNCE_MS,
      );
    });
  }

  protected onCancel() {
    this.dialogRef.close(false);
  }

  protected onSave() {
    const coverUrl = this.coverUrlInput().trim();
    if (!coverUrl) return;
    this.updateMutation.mutate(
      { id: this.playlist.id, name: this.playlist.name, coverUrl },
      {
        onSuccess: () => {
          this.snackbar.open('Playlist updated', undefined, { duration: 2500 });
          this.dialogRef.close(true);
        },
        onError: () =>
          this.snackbar.open('Could not update the playlist', 'Dismiss', { duration: 4000 }),
      },
    );
  }
}
