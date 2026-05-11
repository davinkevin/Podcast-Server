import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
} from '@angular/core';
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
  PlaylistsContainerHAL,
  PlaylistWithItemsHAL,
} from '../../core/models/playlist.model';
import { PageCache } from '../../core/page-cache/page-cache.service';
import { PlaylistCreateDialogComponent } from './playlist-create-dialog.component';

const CACHE_KEY = 'playlists:list';

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
  private readonly pageCache = inject(PageCache);

  protected readonly playlistsResource = this.api.list();

  protected readonly playlistsResult = computed<PlaylistsContainerHAL | undefined>(() => {
    const live = this.playlistsResource.value();
    if (live) return live;
    return this.pageCache.get<PlaylistsContainerHAL>(CACHE_KEY);
  });

  constructor() {
    effect(() => {
      const value = this.playlistsResource.value();
      if (value) this.pageCache.put(CACHE_KEY, value);
    });
  }

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
