import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  ElementRef,
  inject,
  input,
  signal,
  viewChild,
} from '@angular/core';
import { Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { PlaylistEditDialogComponent } from './playlist-edit-dialog.component';

import { CoverCardAction } from '../../shared/cover-card/cover-card.component';
import { TrackRowComponent } from '../../shared/track-row/track-row.component';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { DetailStickyHeaderComponent } from '../../shared/detail-sticky-header/detail-sticky-header.component';
import { PlaylistApi } from '../../core/api/playlist.api';
import {
  PlaylistItemHAL,
  PlaylistWithItemsHAL,
} from '../../core/models/playlist.model';
import { PlayerService } from '../../core/player/player.service';
import {
  applyCoverTint,
  clearCoverTint,
  CoverColorService,
  CoverPalette,
} from '../../core/cover-color/cover-color.service';
import { SettingsService } from '../../core/settings/settings.service';

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
    TrackRowComponent,
    EmptyStateComponent,
    DetailStickyHeaderComponent,
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
  private readonly dialog = inject(MatDialog);
  private readonly player = inject(PlayerService);
  private readonly coverColor = inject(CoverColorService);
  private readonly settings = inject(SettingsService);

  protected readonly id = computed(() => this.idPlaylist());
  protected readonly playlistQuery = this.api.getById(this.id);
  private readonly deleteMutation = this.api.deleteMutation();
  private readonly removeItemMutation = this.api.removeItemMutation();

  // RSS URL is built from the path; the same URL is what podcast clients subscribe to.
  protected readonly rssUrl = computed(() => `${location.origin}/api/v1/playlists/${this.idPlaylist()}/rss`);

  protected readonly cardActions = [REMOVE_ACTION] as const;

  // Palette extracted from the cover via node-vibrant. Pushed onto the global
  // --page-tint / --page-tint-bottom variables so the shell paints a faded
  // gradient across the whole content area (Spotify/Apple Music feel).
  private readonly palette = signal<CoverPalette | null>(null);

  // Per-button accent override (see PodcastDetailComponent for the rationale
  // — Material 19 resolves MDC tokens at theme-compile time so overriding
  // --mat-sys-primary on a parent isn't enough for FAB/filled-button
  // variants; the relevant tokens are set inline).
  protected readonly actionStyles = computed(() => {
    const p = this.palette();
    const primary = p?.vibrant?.hex ?? p?.darkVibrant?.hex;
    const onPrimary = p?.vibrant?.titleText ?? p?.darkVibrant?.titleText;
    if (!primary || !onPrimary) return null;
    return {
      '--mdc-filled-button-container-color': primary,
      '--mdc-filled-button-label-text-color': onPrimary,
      '--mdc-fab-container-color': primary,
      '--mat-fab-foreground-color': onPrimary,
      '--mat-sys-primary': primary,
      '--mat-sys-on-primary': onPrimary,
    };
  });

  // Sticky compact header visibility: true once the hero has scrolled out
  // of view. Driven by an IntersectionObserver on the sentinel placed right
  // after the hero in the template.
  private readonly heroSentinel =
    viewChild<ElementRef<HTMLElement>>('heroSentinel');
  protected readonly heroOffscreen = signal(false);

  constructor() {
    // Extract from the route-derived cover URL (always same-origin, available
    // immediately before the playlist query resolves).
    effect(() => {
      const url = this.coverSrc();
      this.coverColor.extract(url).then((p) => this.palette.set(p));
    });

    effect((onCleanup) => {
      const p = this.palette();
      const dark = this.settings.effectiveTheme() === 'dark';
      applyCoverTint(p, dark);
      onCleanup(() => clearCoverTint());
    });

    // Watch the hero sentinel from the scrollable shell outlet's viewport.
    effect((onCleanup) => {
      const el = this.heroSentinel()?.nativeElement;
      if (!el) return;
      const root = el.closest('.shell__outlet') as HTMLElement | null;
      const observer = new IntersectionObserver(
        ([entry]) => this.heroOffscreen.set(!entry.isIntersecting),
        { root, threshold: 0 },
      );
      observer.observe(el);
      onCleanup(() => observer.disconnect());
    });
  }

  protected coverUrl(playlist: PlaylistWithItemsHAL): string {
    return `/api/v1/playlists/${playlist.id}/cover.jpg`;
  }

  // Bumped after a successful Settings save to bust the browser cache for
  // the cover, whose URL stays the same even after the backend swaps the
  // file on disk.
  protected readonly coverVersion = signal(0);

  // Derived from the route so the cover renders immediately for view-
  // transition morphing, before the playlist resource resolves.
  protected readonly coverSrc = computed(() => {
    const id = this.idPlaylist();
    if (!id) return '';
    const v = this.coverVersion();
    const base = `/api/v1/playlists/${id}/cover.jpg`;
    return v === 0 ? base : `${base}?v=${v}`;
  });

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
    this.router.navigate(['/playlists', this.idPlaylist(), 'items', item.id]);
  }

  protected onAction(item: PlaylistItemHAL, action: CoverCardAction) {
    if (action.id === REMOVE_ACTION.id) this.onRemove(item);
  }

  protected onRemove(item: PlaylistItemHAL) {
    this.removeItemMutation.mutate(
      {
        playlistId: this.idPlaylist(),
        itemId: item.id,
        podcastId: item.podcast.id,
      },
      {
        onSuccess: () =>
          this.snackbar.open('Removed from playlist', undefined, { duration: 2500 }),
        onError: () =>
          this.snackbar.open('Could not remove the item', 'Dismiss', { duration: 4000 }),
      },
    );
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

  protected onOpenSettings(playlist: PlaylistWithItemsHAL) {
    this.dialog
      .open(PlaylistEditDialogComponent, {
        data: playlist,
        autoFocus: 'first-tabbable',
        panelClass: 'ps-fitting-dialog',
      })
      .afterClosed()
      .subscribe((saved) => {
        if (saved) this.coverVersion.update((n) => n + 1);
      });
  }

  protected onDelete(playlist: PlaylistWithItemsHAL) {
    if (!confirm(`Delete playlist "${playlist.name}"?`)) return;
    this.deleteMutation.mutate(playlist.id, {
      onSuccess: () => {
        this.snackbar.open('Playlist deleted', undefined, { duration: 2500 });
        this.router.navigate(['/playlists']);
      },
      onError: () =>
        this.snackbar.open('Could not delete the playlist', 'Dismiss', { duration: 4000 }),
    });
  }
}
