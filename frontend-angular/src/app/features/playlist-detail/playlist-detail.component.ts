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
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { PlaylistEditDialogComponent } from './playlist-edit-dialog.component';

import {
  CoverCardAction,
  CoverCardMenuEntry,
  joinSections,
} from '../../shared/cover-card/cover-card.component';
import { TrackRowComponent } from '../../shared/track-row/track-row.component';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { DetailStickyHeaderComponent } from '../../shared/detail-sticky-header/detail-sticky-header.component';
import {
  StatusBadgeComponent,
  StatusBadgeKind,
} from '../../shared/status-badge/status-badge.component';
import { ItemApi } from '../../core/api/item.api';
import { PlaylistApi } from '../../core/api/playlist.api';
import {
  PlaylistItemHAL,
  PlaylistWithItemsHAL,
} from '../../core/models/playlist.model';
import { DownloadStreamService } from '../../core/downloads/download-stream.service';
import { PlayerService } from '../../core/player/player.service';
import {
  OPEN_IN_VLC_ACTION,
  VlcService,
} from '../../core/vlc/vlc.service';
import {
  applyCoverTint,
  clearCoverTint,
  CoverColorService,
  CoverPalette,
} from '../../core/cover-color/cover-color.service';
import { SettingsService } from '../../core/settings/settings.service';
import { AddToPlaylistDialogComponent } from '../playlists/add-to-playlist-dialog.component';

const PLAY_NEXT_ACTION: CoverCardAction = {
  id: 'play-next',
  label: 'Play next',
  icon: 'queue_play_next',
};
const ADD_TO_QUEUE_ACTION: CoverCardAction = {
  id: 'add-to-queue',
  label: 'Add to queue',
  icon: 'add_to_queue',
};
const REMOVE_FROM_QUEUE_ACTION: CoverCardAction = {
  id: 'remove-from-queue',
  label: 'Remove from queue',
  icon: 'playlist_remove',
};
const STOP_PLAYING_ACTION: CoverCardAction = {
  id: 'stop-playing',
  label: 'Stop playing',
  icon: 'stop_circle',
};
const ADD_TO_PLAYLIST_ACTION: CoverCardAction = {
  id: 'add-to-playlist',
  label: 'Add to another playlist',
  icon: 'playlist_add',
};
const REMOVE_ACTION: CoverCardAction = {
  id: 'remove',
  label: 'Remove from playlist',
  icon: 'playlist_remove',
};
const RESET_ITEM_ACTION: CoverCardAction = {
  id: 'reset-item',
  label: 'Reset',
  icon: 'restart_alt',
};
const DELETE_ITEM_ACTION: CoverCardAction = {
  id: 'delete-item',
  label: 'Delete',
  icon: 'delete',
};

@Component({
  selector: 'ps-playlist-detail',
  standalone: true,
  imports: [
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    TrackRowComponent,
    EmptyStateComponent,
    DetailStickyHeaderComponent,
    StatusBadgeComponent,
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
  private readonly itemApi = inject(ItemApi);
  private readonly stream = inject(DownloadStreamService);
  private readonly snackbar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  protected readonly player = inject(PlayerService);
  private readonly vlc = inject(VlcService);
  private readonly coverColor = inject(CoverColorService);
  private readonly settings = inject(SettingsService);

  protected readonly id = computed(() => this.idPlaylist());
  protected readonly playlistQuery = this.api.getById(this.id);
  private readonly deleteMutation = this.api.deleteMutation();
  private readonly removeItemMutation = this.api.removeItemMutation();
  private readonly resetItemMutation = this.itemApi.resetMutation();
  private readonly deleteItemMutation = this.itemApi.deleteMutation();

  // RSS URL is built from the path; the same URL is what podcast clients
  // subscribe to. Derived from `location.origin`, so it carries the
  // current scheme + host.
  protected readonly rssUrl = computed(() => `${location.origin}/api/v1/playlists/${this.idPlaylist()}/rss`);
  // Standard `https://` (or `http://` in dev) feed URL the Subscribe
  // button points at — same value as `rssUrl`, kept under a dedicated
  // name so the template intent stays clear.
  protected readonly subscribeUrl = this.rssUrl;

  protected actionsFor(item: PlaylistItemHAL): readonly CoverCardMenuEntry[] {
    // Same three-section layout as Library and PodcastDetail (Open + Add
    // to another playlist, queue affordances, then Reset/Delete) plus a
    // playlist-specific Remove-from-playlist next to the rest of the
    // destructive group.
    const header: CoverCardMenuEntry[] = [];
    const openItems: CoverCardAction[] = [];
    if (item.url) {
      openItems.push({
        id: 'open-original',
        label: 'Open original URL',
        icon: 'language',
        url: item.url,
      });
    }
    if (item.isDownloaded) {
      openItems.push({
        id: 'open-file',
        label: 'Open downloaded file',
        icon: 'download',
        url: item.proxyURL,
      });
      openItems.push(OPEN_IN_VLC_ACTION);
    }
    if (openItems.length === 1) {
      header.push(openItems[0]);
    } else if (openItems.length > 1) {
      header.push({
        kind: 'group',
        label: 'Open',
        icon: 'open_in_new',
        items: openItems,
      });
    }
    header.push(ADD_TO_PLAYLIST_ACTION);

    const queue: CoverCardMenuEntry[] = [];
    if (item.isDownloaded) {
      const isCurrent = this.player.currentItem()?.id === item.id;
      if (isCurrent) {
        queue.push(STOP_PLAYING_ACTION);
      } else if (this.player.isQueued(item.id)) {
        queue.push(REMOVE_FROM_QUEUE_ACTION);
      } else {
        queue.push(PLAY_NEXT_ACTION, ADD_TO_QUEUE_ACTION);
      }
    }

    const danger: CoverCardMenuEntry[] = [];
    if (item.isDownloaded) danger.push(RESET_ITEM_ACTION);
    danger.push(REMOVE_ACTION, DELETE_ITEM_ACTION);

    return joinSections(header, queue, danger);
  }

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
    // Template gates Play behind `item.isDownloaded`, but be defensive: an
    // SSE event could land between render and click flipping the state.
    if (!item.isDownloaded) return;
    // Queue the whole playlist starting at the clicked item — the floating
    // player then auto-advances through the rest via `(ended)`. The player
    // service accepts the playlist's narrower `Playable` shape directly,
    // no adapter required.
    const items = this.playlistQuery.data()?.items ?? [item];
    const start = items.findIndex((i) => i.id === item.id);
    this.player.playFromList(items, start >= 0 ? start : 0);
  }

  // True when at least one item in the playlist is actually playable
  // (i.e. downloaded). Drives the disabled state of the header `Play`
  // button so the user can't kick off a queue with nothing to play.
  protected readonly hasPlayableItems = computed(() =>
    (this.playlistQuery.data()?.items ?? []).some((i) => i.isDownloaded),
  );

  protected onPlayAll() {
    const items = this.playlistQuery.data()?.items ?? [];
    // Start at the first downloaded item — `playFromList` itself would
    // skip leading non-downloaded ones, but starting on a known-playable
    // index makes the intent explicit and avoids a wasted no-op.
    const start = items.findIndex((i) => i.isDownloaded);
    if (start < 0) return;
    this.player.playFromList(items, start);
  }

  protected onDownload(item: PlaylistItemHAL) {
    this.itemApi.triggerDownload(item.podcast.id, item.id).subscribe({
      next: () =>
        this.snackbar.open('Added to download queue', undefined, { duration: 2500 }),
      error: () =>
        this.snackbar.open('Could not start download', 'Dismiss', { duration: 4000 }),
    });
  }

  protected statusFor(
    item: PlaylistItemHAL,
  ): { kind: StatusBadgeKind; progression: number | null } | null {
    const downloading = this.stream.downloading().find((d) => d.id === item.id);
    if (downloading) {
      return { kind: 'downloading', progression: downloading.progression };
    }
    if (this.stream.queue().some((q) => q.id === item.id)) {
      return { kind: 'queued', progression: null };
    }
    // Playlist HAL doesn't carry a FAILED status flag — only `isDownloaded`
    // — so we can't surface "failed" badges here. SSE-driven states (queued
    // and downloading) cover the in-flight cases.
    return null;
  }

  protected isInProgress(item: PlaylistItemHAL): boolean {
    return (
      this.stream.downloading().some((d) => d.id === item.id) ||
      this.stream.queue().some((q) => q.id === item.id)
    );
  }

  protected onOpenItem(item: PlaylistItemHAL) {
    this.router.navigate(['/playlists', this.idPlaylist(), 'items', item.id]);
  }

  protected onAction(item: PlaylistItemHAL, action: CoverCardAction) {
    switch (action.id) {
      case PLAY_NEXT_ACTION.id:
        this.player.playNext(item);
        this.snackbar.open('Will play next', undefined, { duration: 2000 });
        break;
      case ADD_TO_QUEUE_ACTION.id:
        this.player.enqueue(item);
        this.snackbar.open('Added to queue', undefined, { duration: 2000 });
        break;
      case REMOVE_FROM_QUEUE_ACTION.id:
        this.player.dequeue(item.id);
        this.snackbar.open('Removed from queue', undefined, { duration: 2000 });
        break;
      case STOP_PLAYING_ACTION.id:
        this.player.close();
        break;
      case ADD_TO_PLAYLIST_ACTION.id:
        this.dialog.open(AddToPlaylistDialogComponent, {
          data: { itemId: item.id, itemTitle: item.title, podcastId: item.podcast.id },
          autoFocus: 'first-tabbable',
          panelClass: 'ps-fitting-dialog',
        });
        break;
      case REMOVE_ACTION.id:
        this.onRemove(item);
        break;
      case OPEN_IN_VLC_ACTION.id:
        this.vlc.openInVlc(item.proxyURL);
        break;
      case RESET_ITEM_ACTION.id:
        this.onResetItem(item);
        break;
      case DELETE_ITEM_ACTION.id:
        this.onDeleteItem(item);
        break;
    }
  }

  private onResetItem(item: PlaylistItemHAL) {
    this.resetItemMutation.mutate(
      { podcastId: item.podcast.id, itemId: item.id },
      {
        onSuccess: () => {
          this.player.closeIf(item.id);
          this.snackbar.open('Item reset', undefined, { duration: 2500 });
        },
        onError: () =>
          this.snackbar.open('Could not reset item', 'Dismiss', { duration: 4000 }),
      },
    );
  }

  private onDeleteItem(item: PlaylistItemHAL) {
    if (!confirm(`Delete "${item.title}"?`)) return;
    this.deleteItemMutation.mutate(
      { podcastId: item.podcast.id, itemId: item.id },
      {
        onSuccess: () => {
          this.player.closeIf(item.id);
          this.snackbar.open('Item deleted', undefined, { duration: 2500 });
        },
        onError: () =>
          this.snackbar.open('Could not delete item', 'Dismiss', { duration: 4000 }),
      },
    );
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
