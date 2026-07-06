import {
  ChangeDetectionStrategy,
  Component,
  computed,
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
import { ItemApi } from '../../core/api/item.api';
import { PlaylistHAL } from '../../core/models/playlist.model';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';

export interface AddToPlaylistDialogData {
  readonly itemId: string;
  readonly itemTitle: string;
  readonly podcastId: string;
}

type Mode = 'pick' | 'create';

@Component({
  selector: 'ps-add-to-playlist-dialog',
  standalone: true,
  imports: [
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    EmptyStateComponent,
  ],
  template: `
    <h2 mat-dialog-title>
      @if (mode() === 'pick') {
        Manage playlists
      } @else {
        New playlist
      }
    </h2>
    <mat-dialog-content class="dialog">
      <p class="dialog__hint" [title]="data.itemTitle">{{ data.itemTitle }}</p>

      @if (mode() === 'pick') {
        @let result = playlistsQuery.data();
        @let loading = playlistsQuery.isPending();
        @if (loading && !result) {
          <div class="dialog__state"><mat-spinner diameter="24" /></div>
        } @else if (result && result.content.length === 0) {
          <ps-empty-state
            icon="playlist_play"
            title="No playlists yet"
            subtitle="Create one below."
          />
        } @else if (result) {
          <ul class="dialog__list">
            @for (p of result.content; track p.id) {
              @let isMember = containingIds().has(p.id);
              <li>
                <button
                  class="row"
                  [class.row--member]="isMember"
                  type="button"
                  [disabled]="busy() === p.id"
                  (click)="onToggle(p, isMember)"
                >
                  <img class="row__cover" [src]="coverUrl(p)" [alt]="p.name" />
                  <span class="row__name">{{ p.name }}</span>
                  @if (busy() === p.id) {
                    <mat-spinner diameter="18" />
                  } @else if (isMember) {
                    <mat-icon class="row__check">check_circle</mat-icon>
                  } @else {
                    <mat-icon class="row__chevron">add</mat-icon>
                  }
                </button>
              </li>
            }
          </ul>
        }
      } @else {
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Name</mat-label>
          <input
            matInput
            required
            cdkFocusInitial
            [ngModel]="newName()"
            (ngModelChange)="newName.set($event)"
            (keydown.enter)="onCreateAndAdd()"
            [disabled]="creating()"
          />
        </mat-form-field>

        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Cover URL (optional)</mat-label>
          <input
            matInput
            type="url"
            [ngModel]="newCoverUrl()"
            (ngModelChange)="newCoverUrl.set($event)"
            [disabled]="creating()"
          />
          <mat-hint>Leave empty for the default placeholder cover.</mat-hint>
        </mat-form-field>
      }
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      @if (mode() === 'pick') {
        <button mat-stroked-button color="primary" (click)="mode.set('create')">
          <mat-icon>add</mat-icon>
          New playlist
        </button>
        <button mat-button (click)="onClose()">Done</button>
      } @else {
        <button mat-button [disabled]="creating()" (click)="mode.set('pick')">
          Back
        </button>
        <button
          mat-flat-button
          color="primary"
          [disabled]="!newName().trim() || creating()"
          (click)="onCreateAndAdd()"
        >
          @if (creating()) {
            <mat-spinner diameter="18" />
          } @else {
            <mat-icon>add</mat-icon>
          }
          Create &amp; add
        </button>
      }
    </mat-dialog-actions>
  `,
  styles: `
    .dialog {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
      min-width: min(28rem, 90vw);
    }
    .dialog > :first-child {
      margin-top: 1.5rem;
    }
    .dialog__hint {
      font: var(--mat-sys-body-medium);
      color: var(--mat-sys-on-surface-variant);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      margin: 0;
    }
    .dialog__state {
      padding: 1.5rem;
      display: flex;
      justify-content: center;
    }
    .dialog__list {
      list-style: none;
      margin: 0;
      padding: 0;
      max-height: 360px;
      overflow-y: auto;
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }
    .row {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      width: 100%;
      padding: 0.5rem;
      background: transparent;
      border: 0;
      border-radius: 12px;
      cursor: pointer;
      color: inherit;
      text-align: left;
      font: inherit;
    }
    .row:hover:not(:disabled) {
      background: var(--mat-sys-surface-container-highest);
    }
    .row:disabled {
      opacity: 0.6;
      cursor: default;
    }
    .row--member {
      background: color-mix(in srgb, var(--mat-sys-primary-container) 35%, transparent);
    }
    .row--member:hover:not(:disabled) {
      background: color-mix(in srgb, var(--mat-sys-primary-container) 55%, transparent);
    }
    .row__cover {
      width: 40px;
      height: 40px;
      object-fit: cover;
      border-radius: 8px;
      flex: 0 0 auto;
      display: block;
    }
    .row__name {
      flex: 1 1 auto;
      min-width: 0;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .row__chevron {
      color: var(--mat-sys-on-surface-variant);
    }
    .row__check {
      color: var(--mat-sys-primary);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AddToPlaylistDialogComponent {
  protected readonly data = inject<AddToPlaylistDialogData>(MAT_DIALOG_DATA);
  private readonly playlists = inject(PlaylistApi);
  private readonly items = inject(ItemApi);
  private readonly dialogRef = inject(
    MatDialogRef<AddToPlaylistDialogComponent, boolean>,
  );
  private readonly snackbar = inject(MatSnackBar);

  protected readonly mode = signal<Mode>('pick');
  protected readonly playlistsQuery = this.playlists.list();
  private readonly containingQuery = this.items.playlistsContaining(
    signal({ podcastId: this.data.podcastId, itemId: this.data.itemId }),
  );
  protected readonly containingIds = computed(
    () => new Set(this.containingQuery.data()?.content.map((p) => p.id) ?? []),
  );
  private readonly addItemMutation = this.playlists.addItemMutation();
  private readonly removeItemMutation = this.playlists.removeItemMutation();
  private readonly createMutation = this.playlists.createMutation();
  protected readonly busy = signal<string | undefined>(undefined);
  // Dialog returns a "changed" flag to callers that want to reload state.
  private changed = false;

  protected readonly newName = signal<string>('');
  protected readonly newCoverUrl = signal<string>('');
  protected readonly creating = this.createMutation.isPending;

  protected coverUrl(p: PlaylistHAL): string {
    return `/api/v1/playlists/${p.id}/cover.jpg`;
  }

  protected onClose() {
    this.dialogRef.close(this.changed);
  }

  protected onToggle(playlist: PlaylistHAL, isMember: boolean) {
    if (this.busy()) return;
    this.busy.set(playlist.id);
    const vars = {
      playlistId: playlist.id,
      itemId: this.data.itemId,
      podcastId: this.data.podcastId,
    };
    const mutation = isMember ? this.removeItemMutation : this.addItemMutation;
    mutation.mutate(vars, {
      onSuccess: () => {
        this.busy.set(undefined);
        this.changed = true;
        this.snackbar.open(
          isMember
            ? `Removed from "${playlist.name}"`
            : `Added to "${playlist.name}"`,
          undefined,
          { duration: 2500 },
        );
      },
      onError: () => {
        this.busy.set(undefined);
        this.snackbar.open(
          isMember ? 'Could not remove' : 'Could not add',
          'Dismiss',
          { duration: 4000 },
        );
      },
    });
  }

  protected onCreateAndAdd() {
    const name = this.newName().trim();
    if (!name) return;
    this.createMutation.mutate(
      { name, coverUrl: this.newCoverUrl().trim() || undefined },
      {
        onSuccess: (created) => {
          this.addItemMutation.mutate(
            {
              playlistId: created.id,
              itemId: this.data.itemId,
              podcastId: this.data.podcastId,
            },
            {
              onSuccess: () => {
                this.changed = true;
                this.snackbar.open(`Added to "${created.name}"`, undefined, { duration: 2500 });
                this.newName.set('');
                this.newCoverUrl.set('');
                this.mode.set('pick');
              },
              onError: () =>
                this.snackbar.open(
                  `Playlist "${created.name}" was created, but the item could not be added.`,
                  'Dismiss',
                  { duration: 4000 },
                ),
            },
          );
        },
        onError: () =>
          this.snackbar.open('Could not create playlist', 'Dismiss', { duration: 4000 }),
      },
    );
  }
}
