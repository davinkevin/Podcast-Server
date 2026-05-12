import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
} from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDialog } from '@angular/material/dialog';
import { QueryClient } from '@tanstack/angular-query-experimental';

import { CoverCardComponent } from '../../shared/cover-card/cover-card.component';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { PlaylistApi } from '../../core/api/playlist.api';
import {
  PlaylistHAL,
  PlaylistWithItemsHAL,
} from '../../core/models/playlist.model';
import { queryKeys } from '../../core/api/query-keys';
import { PlaylistCreateDialogComponent } from './playlist-create-dialog.component';
import { PlaylistsListStateService } from './playlists-list-state.service';

@Component({
  selector: 'ps-playlists',
  standalone: true,
  imports: [
    FormsModule,
    MatProgressSpinnerModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
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
  private readonly queryClient = inject(QueryClient);
  private readonly state = inject(PlaylistsListStateService);

  protected readonly playlistsQuery = this.api.list();

  // Client-side search backed by a session-scoped service so the filter
  // survives navigating away and back. `GET /api/v1/playlists` returns
  // everything in one shot — filtering by name is purely UI.
  protected readonly search = this.state.search;

  protected readonly visiblePlaylists = computed<readonly PlaylistHAL[]>(() => {
    const data = this.playlistsQuery.data();
    if (!data) return [];
    const q = this.search().trim().toLowerCase();
    if (!q) return data.content;
    return data.content.filter((p) => p.name.toLowerCase().includes(q));
  });

  protected coverUrl(p: PlaylistHAL): string {
    return `/api/v1/playlists/${p.id}/cover.jpg`;
  }

  protected onOpen(p: PlaylistHAL) {
    this.router.navigate(['/playlists', p.id]);
  }

  protected onClearSearch() {
    this.search.set('');
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
        this.queryClient.invalidateQueries({
          queryKey: queryKeys.playlists.list(),
        });
        this.router.navigate(['/playlists', created.id]);
      });
  }
}
