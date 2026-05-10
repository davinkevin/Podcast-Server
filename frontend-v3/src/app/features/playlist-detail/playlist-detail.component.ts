import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  signal,
} from '@angular/core';
import { Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import {
  CoverCardAction,
  CoverCardComponent,
} from '../../shared/cover-card/cover-card.component';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { PlaylistApi } from '../../core/api/playlist.api';
import {
  PlaylistItemHAL,
  PlaylistWithItemsHAL,
} from '../../core/models/playlist.model';
import { PlayerService } from '../../core/player/player.service';

const REMOVE_ACTION: CoverCardAction = {
  id: 'remove',
  label: 'Remove from playlist',
  icon: 'playlist_remove',
};

@Component({
  selector: 'ps-playlist-detail',
  standalone: true,
  imports: [
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatProgressSpinnerModule,
    CoverCardComponent,
    EmptyStateComponent,
  ],
  templateUrl: './playlist-detail.component.html',
  styleUrl: './playlist-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class PlaylistDetailComponent {
  // Bound from /playlists/:idPlaylist via withComponentInputBinding().
  readonly idPlaylist = input.required<string>();

  private readonly router = inject(Router);
  private readonly api = inject(PlaylistApi);
  private readonly snackbar = inject(MatSnackBar);
  private readonly player = inject(PlayerService);

  protected readonly id = computed(() => this.idPlaylist());
  protected readonly playlistResource = this.api.getById(this.id);

  // RSS URL is built from the path; the same URL is what podcast clients subscribe to.
  protected readonly rssUrl = computed(() => `${location.origin}/api/v1/playlists/${this.idPlaylist()}/rss`);

  protected readonly cardActions = [REMOVE_ACTION] as const;

  protected coverUrl(playlist: PlaylistWithItemsHAL): string {
    return `/api/v1/playlists/${playlist.id}/cover.jpg`;
  }

  protected itemCoverUrl(item: PlaylistItemHAL): string {
    return item.cover.url;
  }

  protected onPlay(item: PlaylistItemHAL) {
    // Playlist items don't carry a status; we trust the user added them on
    // purpose and treat them as playable. PlayerService.open requires
    // `isDownloaded`, so go through a thin shim that flips that flag.
    this.player.open({
      id: item.id,
      title: item.title,
      url: item.proxyURL,
      pubDate: null,
      downloadDate: null,
      creationDate: '',
      description: item.description ?? '',
      mimeType: item.mimeType,
      length: null,
      fileName: null,
      status: 'FINISH',
      podcast: { id: item.podcast.id, title: item.podcast.title, url: '' },
      cover: {
        id: item.cover.id,
        width: item.cover.width,
        height: item.cover.height,
        url: item.cover.url,
      },
      isDownloaded: true,
      podcastId: item.podcast.id,
      proxyURL: item.proxyURL,
    });
  }

  protected onOpenItem(item: PlaylistItemHAL) {
    this.router.navigate(['/podcasts', item.podcast.id, 'items', item.id]);
  }

  protected onAction(item: PlaylistItemHAL, action: CoverCardAction) {
    if (action.id === REMOVE_ACTION.id) this.onRemove(item);
  }

  protected onRemove(item: PlaylistItemHAL) {
    this.api.removeItem(this.idPlaylist(), item.id).subscribe({
      next: () => {
        this.snackbar.open('Removed from playlist', undefined, { duration: 2500 });
        this.playlistResource.reload();
      },
      error: () =>
        this.snackbar.open('Could not remove the item', 'Dismiss', { duration: 4000 }),
    });
  }

  protected readonly copyHint = signal<'idle' | 'copied'>('idle');

  protected onCopyRss() {
    const url = this.rssUrl();
    navigator.clipboard.writeText(url).then(
      () => {
        this.copyHint.set('copied');
        this.snackbar.open('RSS URL copied', undefined, { duration: 2500 });
        setTimeout(() => this.copyHint.set('idle'), 2500);
      },
      () =>
        this.snackbar.open('Could not copy the URL', 'Dismiss', { duration: 4000 }),
    );
  }

  protected onDelete(playlist: PlaylistWithItemsHAL) {
    if (!confirm(`Delete playlist "${playlist.name}"?`)) return;
    this.api.delete(playlist.id).subscribe({
      next: () => {
        this.snackbar.open('Playlist deleted', undefined, { duration: 2500 });
        this.router.navigate(['/playlists']);
      },
      error: () =>
        this.snackbar.open('Could not delete the playlist', 'Dismiss', { duration: 4000 }),
    });
  }
}
