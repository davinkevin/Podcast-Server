import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { PlaylistApi } from '../../core/api/playlist.api';
import { PlaylistWithItemsHAL } from '../../core/models/playlist.model';

@Component({
  selector: 'ps-playlist-create-dialog',
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
    <h2 mat-dialog-title>Create playlist</h2>
    <mat-dialog-content class="dialog">
      <mat-form-field appearance="outline" subscriptSizing="dynamic">
        <mat-label>Name</mat-label>
        <input
          matInput
          required
          cdkFocusInitial
          [ngModel]="name()"
          (ngModelChange)="name.set($event)"
          (keydown.enter)="onCreate()"
          [disabled]="creating()"
        />
      </mat-form-field>

      <mat-form-field appearance="outline" subscriptSizing="dynamic">
        <mat-label>Cover URL (optional)</mat-label>
        <input
          matInput
          type="url"
          [ngModel]="coverUrl()"
          (ngModelChange)="coverUrl.set($event)"
          [disabled]="creating()"
        />
        <mat-hint>Leave empty for the default placeholder cover.</mat-hint>
      </mat-form-field>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button [disabled]="creating()" (click)="onCancel()">Cancel</button>
      <button
        mat-flat-button
        color="primary"
        [disabled]="!name().trim() || creating()"
        (click)="onCreate()"
      >
        @if (creating()) {
          <mat-spinner diameter="18" />
        } @else {
          <mat-icon>add</mat-icon>
        }
        Create
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .dialog {
      display: flex;
      flex-direction: column;
      gap: 1.25rem;
      min-width: min(28rem, 90vw);
    }
    .dialog > :first-child {
      margin-top: 1.5rem;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlaylistCreateDialogComponent {
  private readonly api = inject(PlaylistApi);
  private readonly snackbar = inject(MatSnackBar);
  private readonly dialogRef = inject(
    MatDialogRef<PlaylistCreateDialogComponent, PlaylistWithItemsHAL>,
  );

  protected readonly name = signal<string>('');
  protected readonly coverUrl = signal<string>('');
  private readonly createMutation = this.api.createMutation();
  protected readonly creating = this.createMutation.isPending;

  protected onCancel() {
    this.dialogRef.close();
  }

  protected onCreate() {
    const n = this.name().trim();
    if (!n) return;
    this.createMutation.mutate(
      { name: n, coverUrl: this.coverUrl().trim() || undefined },
      {
        onSuccess: (created) => {
          this.snackbar.open('Playlist created', undefined, { duration: 2500 });
          this.dialogRef.close(created);
        },
        onError: () =>
          this.snackbar.open('Could not create playlist', 'Dismiss', { duration: 4000 }),
      },
    );
  }
}
