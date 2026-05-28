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
import {
  NavigationOrigin,
  NavigationOriginService,
} from '../../core/navigation/navigation-origin.service';
import { Router, RouterLink } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { DatePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';

import { ItemApi, ItemRef } from '../../core/api/item.api';
import { PlaylistApi } from '../../core/api/playlist.api';
import { ItemHAL } from '../../core/models/item.model';
import { PlayerService } from '../../core/player/player.service';
import { DownloadStreamService } from '../../core/downloads/download-stream.service';
import {
  applyCoverTint,
  clearCoverTint,
  CoverColorService,
  CoverPalette,
} from '../../core/cover-color/cover-color.service';
import { SettingsService } from '../../core/settings/settings.service';
import { VlcService } from '../../core/vlc/vlc.service';
import {
  StatusBadgeComponent,
  StatusBadgeKind,
} from '../../shared/status-badge/status-badge.component';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { AddToPlaylistDialogComponent } from '../playlists/add-to-playlist-dialog.component';

@Component({
  selector: 'ps-item-detail',
  standalone: true,
  imports: [
    RouterLink,
    DatePipe,
    MatIconModule,
    MatButtonModule,
    MatDividerModule,
    MatMenuModule,
    MatProgressSpinnerModule,
    StatusBadgeComponent,
    EmptyStateComponent,
  ],
  templateUrl: './item-detail.component.html',
  styleUrl: './item-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class ItemDetailComponent {
  // One of `idPodcast` / `idPlaylist` is set depending on which route matched:
  //   /podcasts/:idPodcast/items/:id   → idPodcast set, idPlaylist undefined
  //   /playlists/:idPlaylist/items/:id → idPlaylist set, idPodcast undefined
  // In the playlist case the podcastId is derived from the playlist payload
  // (each PlaylistItemHAL carries its parent podcast).
  readonly idPodcast = input<string | undefined>(undefined);
  readonly idPlaylist = input<string | undefined>(undefined);
  readonly id = input.required<string>();

  // For the podcast/library origin, the source list seeded an in-memory
  // marker right before navigating. Consumed once here to scope the hero
  // cover's view-transition-name to that list only. The playlist origin is
  // conveyed by the URL itself so we infer it without the service.
  private readonly initialNavOrigin: NavigationOrigin | null =
    inject(NavigationOriginService).consume();

  protected readonly origin = computed<NavigationOrigin | null>(() =>
    this.idPlaylist() ? 'playlist' : this.initialNavOrigin,
  );

  protected readonly heroTransitionName = computed<string | null>(() => {
    const o = this.origin();
    if (!o) return null;
    return `${o}-item-cover-${this.id()}`;
  });

  private readonly itemApi = inject(ItemApi);
  private readonly playlistApi = inject(PlaylistApi);
  protected readonly player = inject(PlayerService);
  private readonly stream = inject(DownloadStreamService);
  private readonly router = inject(Router);
  private readonly title = inject(Title);
  private readonly snackbar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  private readonly coverColor = inject(CoverColorService);
  private readonly settings = inject(SettingsService);
  private readonly vlc = inject(VlcService);

  // Playlist payload is fetched only when arrived via the /playlists route;
  // it gives us the parent podcastId (each item carries its podcast) plus the
  // playlist name & cover for the contextual UI bits (pre-title link, mini
  // cover overlay, removal action).
  protected readonly playlistQuery = this.playlistApi.getById(this.idPlaylist);

  protected readonly fromPlaylist = computed(() => !!this.idPlaylist());

  // The playlist item that points at this item id. Used for the cover URL
  // fallback (so the hero can render before itemQuery resolves) and to
  // resolve podcastId.
  protected readonly playlistItem = computed(() => {
    const pl = this.playlistQuery.data();
    return pl?.items.find((i) => i.id === this.id()) ?? null;
  });

  // Effective podcastId — direct from the route in the podcast case, derived
  // from the playlist payload in the playlist case. Undefined while the
  // playlist query is in flight.
  protected readonly podcastId = computed<string | undefined>(
    () => this.idPodcast() ?? this.playlistItem()?.podcast.id,
  );

  // Empty-state guard for the rare case of a stale URL pointing to an item
  // no longer in the playlist. True only once the playlist has resolved.
  protected readonly itemMissingFromPlaylist = computed(() => {
    if (!this.fromPlaylist()) return false;
    if (!this.playlistQuery.isSuccess()) return false;
    return !this.playlistItem();
  });

  protected readonly ref = computed<ItemRef | undefined>(() => {
    const podcastId = this.podcastId();
    if (!podcastId) return undefined;
    return { podcastId, id: this.id() };
  });
  protected readonly itemQuery = this.itemApi.getById(this.ref);
  private readonly resetMutation = this.itemApi.resetMutation();
  private readonly deleteMutation = this.itemApi.deleteMutation();
  private readonly removeFromPlaylistMutation =
    this.playlistApi.removeItemMutation();

  // CSS `aspect-ratio` value piped onto the cover wrapper so it matches the
  // artwork's natural shape (16/9 for YouTube, 1/1 for most RSS items, …).
  // Null while the HAL hasn't resolved — SCSS keeps the 1/1 default in
  // that window so the view-transition into this page has a stable target.
  protected readonly coverAspectRatio = computed<string | null>(() => {
    const item = this.itemQuery.data();
    if (!item) return null;
    return `${item.cover.width} / ${item.cover.height}`;
  });

  // Layout switch: wide covers (≥ 16/9) stack the artwork above the meta
  // column at full page width; everything else keeps the side-by-side
  // layout where the cover sits in a fixed-width left column.
  protected readonly coverIsWide = computed(() => {
    const item = this.itemQuery.data();
    if (!item) return false;
    return item.cover.width / item.cover.height >= 16 / 9;
  });

  // Cover URL — must be ready before itemQuery resolves to keep the
  // view-transition morph smooth. Priority: the resolved item, then the
  // playlist item (when we came from a playlist and have the playlist
  // payload), then the route-derived URL (podcast case).
  protected readonly coverSrc = computed(() => {
    const item = this.itemQuery.data();
    if (item) return item.cover.proxyURL;
    const fromPl = this.playlistItem();
    if (fromPl) return fromPl.cover.proxyURL;
    const podcastId = this.podcastId();
    if (podcastId) return `/api/v1/podcasts/${podcastId}/items/${this.id()}/cover.jpg`;
    return '';
  });

  // Cover of the playlist itself, shown as a small overlay on the hero when
  // the user arrived via /playlists/:idPlaylist/items/:id — signature
  // visual cue that we're inside a playlist context.
  protected readonly playlistCoverSrc = computed(() =>
    this.idPlaylist() ? `/api/v1/playlists/${this.idPlaylist()}/cover.jpg` : null,
  );

  // Palette from the item's cover, propagated via --page-tint / --page-tint-
  // bottom so the whole content area picks up the color like the podcast page.
  private readonly palette = signal<CoverPalette | null>(null);

  // Per-button accent override. Material 19 uses per-component MDC tokens
  // (--mdc-filled-button-container-color, --mdc-fab-container-color, …)
  // — overriding --mat-sys-primary on a parent isn't enough since those
  // tokens are resolved at theme-compile time. We set the relevant tokens
  // inline so flat-button and fab variants pick up the cover accent.
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

  constructor() {
    effect(() => {
      const item = this.itemQuery.data();
      if (item) this.title.setTitle(`${item.title} — Podcast Server`);
    });

    effect(() => {
      const item = this.itemQuery.data();
      if (!item) return;
      this.coverColor.extract(item.cover.proxyURL).then((p) => this.palette.set(p));
    });

    effect((onCleanup) => {
      const p = this.palette();
      const dark = this.settings.effectiveTheme() === 'dark';
      applyCoverTint(p, dark);
      onCleanup(() => clearCoverTint());
    });
  }

  protected statusFor(item: ItemHAL): { kind: StatusBadgeKind; progression: number | null } | null {
    const downloading = this.stream.downloading().find((d) => d.id === item.id);
    if (downloading) {
      return { kind: 'downloading', progression: downloading.progression };
    }
    if (this.stream.queue().some((q) => q.id === item.id)) {
      return { kind: 'queued', progression: null };
    }
    if (item.status === 'FAILED') {
      return { kind: 'failed', progression: null };
    }
    return null;
  }

  protected isInProgress(item: ItemHAL): boolean {
    return (
      this.stream.downloading().some((d) => d.id === item.id) ||
      this.stream.queue().some((q) => q.id === item.id)
    );
  }

  protected onPlay(item: ItemHAL) {
    // In playlist mode, queue every playable item from the playlist and
    // start at the current one — the floating player then auto-advances
    // through the rest via the native `ended` event. In podcast mode we
    // open a single-item queue, same effective behaviour as before.
    const playlist = this.playlistQuery.data();
    if (playlist) {
      const startIndex = playlist.items.findIndex((i) => i.id === item.id);
      if (startIndex >= 0) {
        this.player.playFromList(playlist.items, startIndex);
        return;
      }
    }
    this.player.open(item);
  }

  protected onPlayNext(item: ItemHAL) {
    this.player.playNext(item);
    this.snackbar.open('Will play next', undefined, { duration: 2000 });
  }

  protected onAddToQueue(item: ItemHAL) {
    this.player.enqueue(item);
    this.snackbar.open('Added to queue', undefined, { duration: 2000 });
  }

  protected onRemoveFromQueue(item: ItemHAL) {
    this.player.dequeue(item.id);
    this.snackbar.open('Removed from queue', undefined, { duration: 2000 });
  }

  protected onStopPlaying() {
    this.player.close();
  }

  protected onDownload(item: ItemHAL) {
    this.itemApi.triggerDownload(item.podcastId, item.id).subscribe({
      next: () => this.snackbar.open('Added to download queue', undefined, { duration: 2500 }),
      error: () => this.snackbar.open('Could not start download', 'Dismiss', { duration: 4000 }),
    });
  }

  protected onReset(item: ItemHAL) {
    this.resetMutation.mutate(
      { podcastId: item.podcastId, itemId: item.id },
      {
        onSuccess: () => {
          // Reset deletes the file on disk; if it's playing, the proxyURL would 404 — close.
          this.player.closeIf(item.id);
          this.snackbar.open('Item reset', undefined, { duration: 2500 });
        },
        onError: () =>
          this.snackbar.open('Could not reset item', 'Dismiss', { duration: 4000 }),
      },
    );
  }

  protected onDelete(item: ItemHAL) {
    if (!confirm(`Delete "${item.title}"?`)) return;
    this.deleteMutation.mutate(
      { podcastId: item.podcastId, itemId: item.id },
      {
        onSuccess: () => {
          this.player.closeIf(item.id);
          this.snackbar.open('Item deleted', undefined, { duration: 2500 });
          this.router.navigate(['/library']);
        },
        onError: () =>
          this.snackbar.open('Could not delete item', 'Dismiss', { duration: 4000 }),
      },
    );
  }

  protected onRemoveFromPlaylist(item: ItemHAL) {
    const playlistId = this.idPlaylist();
    if (!playlistId) return;
    this.removeFromPlaylistMutation.mutate(
      { playlistId, itemId: item.id, podcastId: item.podcastId },
      {
        onSuccess: () => {
          this.snackbar.open('Removed from playlist', undefined, { duration: 2500 });
          this.router.navigate(['/playlists', playlistId]);
        },
        onError: () =>
          this.snackbar.open('Could not remove from playlist', 'Dismiss', { duration: 4000 }),
      },
    );
  }

  protected onOpenInVlc(item: ItemHAL) {
    this.vlc.openInVlc(item.proxyURL);
  }

  protected onAddToPlaylist(item: ItemHAL) {
    this.dialog.open(AddToPlaylistDialogComponent, {
      data: { itemId: item.id, itemTitle: item.title, podcastId: item.podcastId },
      autoFocus: 'first-tabbable',
      panelClass: 'ps-fitting-dialog',
    });
  }

  // Reference to the hero <img>. We re-tag its view-transition-name right
  // before the user navigates to the parent podcast so the cover morphs
  // into this same item's row in the podcast-detail episode list (same
  // artwork on both ends — would be incoherent to morph it into the
  // podcast's own hero cover since the two images can differ).
  protected readonly coverEl =
    viewChild<ElementRef<HTMLImageElement>>('coverEl');

  protected onPodcastLinkClick(event: MouseEvent) {
    // Leave modified clicks (open in new tab, save link as, …) alone — they
    // don't trigger the SPA navigation that would consume the snapshot.
    if (
      event.button !== 0 ||
      event.metaKey ||
      event.ctrlKey ||
      event.shiftKey ||
      event.altKey
    )
      return;
    const cover = this.coverEl()?.nativeElement;
    // Match the name podcast-detail puts on the row for this item — when
    // the destination renders, the two snapshots morph together. In the
    // podcast-origin case the hero already carries this name (set via
    // heroTransitionName), so this is a no-op there.
    if (cover) cover.style.viewTransitionName = `podcast-item-cover-${this.id()}`;
  }
}
