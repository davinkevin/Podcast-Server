import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';

import { CoverCardComponent } from '../../shared/cover-card/cover-card.component';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { PlaylistApi } from '../../core/api/playlist.api';
import {
  PlaylistHAL,
  PlaylistWithItemsHAL,
} from '../../core/models/playlist.model';
import { PlaylistCreateDialogComponent } from './playlist-create-dialog.component';

@Component({
  selector: 'ps-playlists',
  standalone: true,
  imports: [
    MatProgressSpinnerModule,
    MatButtonModule,
    MatIconModule,
    CoverCardComponent,
    EmptyStateComponent,
  ],
  templateUrl: './playlists.component.html',
  styleUrl: './playlists.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class PlaylistsComponent {
  private readonly router = inject(Router);
  private readonly api = inject(PlaylistApi);
  private readonly dialog = inject(MatDialog);

  protected readonly playlistsResource = this.api.list();

  protected coverUrl(p: PlaylistHAL): string {
    return `/api/v1/playlists/${p.id}/cover.jpg`;
  }

  protected onOpen(p: PlaylistHAL) {
    this.router.navigate(['/playlists', p.id]);
  }

  protected onCreate() {
    this.dialog
      .open(PlaylistCreateDialogComponent, {
        autoFocus: 'first-tabbable',
        panelClass: 'ps-fitting-dialog',
      })
      .afterClosed()
      .subscribe((created: PlaylistWithItemsHAL | undefined) => {
        if (!created) return;
        this.playlistsResource.reload();
        this.router.navigate(['/playlists', created.id]);
      });
  }
}
