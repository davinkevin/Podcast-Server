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
import { CdkConnectedOverlay, OverlayModule } from '@angular/cdk/overlay';
import { QueryClient } from '@tanstack/angular-query-experimental';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatBadgeModule } from '@angular/material/badge';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import {
  CoverCardAction,
  CoverCardMenuEntry,
  joinSections,
} from '../../shared/cover-card/cover-card.component';
import { TrackRowComponent } from '../../shared/track-row/track-row.component';
import { PagerComponent } from '../../shared/pager/pager.component';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import {
  ItemFilters,
  ItemFiltersPanelComponent,
} from '../../shared/item-filters/item-filters-panel.component';
import {
  STATUS_FILTER_OPTIONS,
  StatusFilter,
  isStatusFilter,
  statusesFor,
} from '../../shared/item-filters/item-filters.model';
import { DetailStickyHeaderComponent } from '../../shared/detail-sticky-header/detail-sticky-header.component';
import {
  StatusBadgeComponent,
  StatusBadgeKind,
} from '../../shared/status-badge/status-badge.component';
import { PodcastApi, PodcastItemsInput } from '../../core/api/podcast.api';
import { ItemApi } from '../../core/api/item.api';
import { ItemHAL } from '../../core/models/item.model';
import { PodcastHAL } from '../../core/models/podcast.model';
import { DownloadStreamService } from '../../core/downloads/download-stream.service';
import { PlayerService } from '../../core/player/player.service';
import { CoverColorService, CoverPalette } from '../../core/cover-color/cover-color.service';
import { PageTintService } from '../../core/cover-color/page-tint.service';
import { CurrentUrlService } from '../../core/navigation/current-url.service';
import { NavigationOriginService } from '../../core/navigation/navigation-origin.service';
import { OPEN_IN_VLC_ACTION, VlcService } from '../../core/vlc/vlc.service';
import { queryKeys } from '../../core/api/query-keys';

import { PodcastEditDialogComponent } from './podcast-edit-dialog.component';
import { PodcastUploadDialogComponent } from './podcast-upload-dialog.component';
import { AddToPlaylistDialogComponent } from '../playlists/add-to-playlist-dialog.component';

const DEFAULT_PAGE_SIZE = 24;
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
  label: 'Add to playlist',
  icon: 'playlist_add',
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
  selector: 'ps-podcast-detail',
  standalone: true,
  imports: [
    DatePipe,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatProgressSpinnerModule,
    TrackRowComponent,
    PagerComponent,
    EmptyStateComponent,
    DetailStickyHeaderComponent,
    StatusBadgeComponent,
    OverlayModule,
    MatBadgeModule,
    MatChipsModule,
    ItemFiltersPanelComponent,
  ],
  templateUrl: './podcast-detail.component.html',
  styleUrl: './podcast-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class PodcastDetailComponent {
  // Bound from /podcasts/:idPodcast via withComponentInputBinding().
  readonly idPodcast = input.required<string>();
  readonly page = input<number, string | number>(0, {
    transform: (v) => {
      const n = typeof v === 'number' ? v : Number.parseInt(v, 10);
      return Number.isFinite(n) && n >= 0 ? n : 0;
    },
  });
  // Search query — bound from `?q=` so the URL stays the source of truth
  // (mirrors the Library pattern). Empty string when absent.
  readonly q = input<string>('');
  /**
   * Bound from `?status=`, carrying the app's three-way vocabulary rather than
   * the API's status list, exactly as the library does. The transform accepts
   * `undefined` because `withComponentInputBinding` sets every input on each
   * navigation, passing it for a parameter the URL does not have.
   *
   * There is no `?tags=` here: tags belong to the podcast, not the item, so
   * inside one podcast they would match everything or nothing.
   */
  readonly status = input<StatusFilter, string | undefined>('all', {
    transform: (v) => (isStatusFilter(v) ? v : 'all'),
  });

  private readonly filtersTrigger = viewChild('filtersTrigger', {
    read: ElementRef<HTMLElement>,
  });
  private readonly filtersOverlay = viewChild(CdkConnectedOverlay);

  private readonly router = inject(Router);
  private readonly currentUrl = inject(CurrentUrlService);
  private readonly api = inject(PodcastApi);
  private readonly itemApi = inject(ItemApi);
  private readonly stream = inject(DownloadStreamService);
  protected readonly player = inject(PlayerService);
  private readonly dialog = inject(MatDialog);
  private readonly snackbar = inject(MatSnackBar);
  private readonly coverColor = inject(CoverColorService);
  private readonly queryClient = inject(QueryClient);
  private readonly navOrigin = inject(NavigationOriginService);
  private readonly vlc = inject(VlcService);
  private readonly pageTint = inject(PageTintService);
  private readonly hostElement = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly id = computed(() => this.idPodcast());
  protected readonly podcastQuery = this.api.getById(this.id);
  private readonly deleteMutation = this.api.deleteMutation();
  private readonly resetItemMutation = this.itemApi.resetMutation();
  private readonly deleteItemMutation = this.itemApi.deleteMutation();

  /* Bumped on every successful Settings save to bust the browser cache for the
     cover, whose URL stays the same (`/api/v1/podcasts/{id}/cover.jpg`) even
     after the backend swaps the file on disk. */
  protected readonly coverVersion = signal(0);
  // Derive cover URL from the route param so the cover renders before the
  // podcast resource resolves — required for view-transition morphs from the
  // list page to have a destination element in the new DOM snapshot.
  protected readonly coverSrc = computed(() => {
    const id = this.idPodcast();
    if (!id) return '';
    const v = this.coverVersion();
    const base = `/api/v1/podcasts/${id}/cover.jpg`;
    return v === 0 ? base : `${base}?v=${v}`;
  });

  protected readonly itemsInput = computed<PodcastItemsInput>(() => ({
    podcastId: this.idPodcast(),
    q: this.q(),
    page: this.page(),
    size: DEFAULT_PAGE_SIZE,
    // `all` expands to an empty list, which the API layer omits, so an
    // unfiltered request is byte-for-byte what it was before.
    status: statusesFor(this.status()),
  }));
  protected readonly itemsQuery = this.api.items(this.itemsInput);

  // True while this exact podcast is being refreshed server-side — the
  // backend emits `podcast-updating` SSE events around its update run
  // and `DownloadStreamService` accumulates them. Used to swap the
  // "Update now" button for a spinner + disabled state.
  protected readonly isUpdating = computed(() =>
    this.stream.updatingPodcasts().has(this.idPodcast()),
  );

  // RSS URL exposed by the backend. The same URL drives both the
  // clipboard fallback and the subscribe link. Derived from
  // `location.origin`, so it carries the current scheme + host.
  protected readonly rssUrl = computed(
    () => `${location.origin}/api/v1/podcasts/${this.idPodcast()}/rss`,
  );
  // Standard `https://` (or `http://` in dev) feed URL the Subscribe
  // button points at — same value as `rssUrl`, kept under a dedicated
  // name so the template intent stays clear and the full-feed variant
  // below can build on it.
  protected readonly subscribeUrl = this.rssUrl;
  // Full-feed variant — the backend's RSS endpoint defaults to a capped
  // recent window; appending `?limit=false` returns every episode the
  // podcast has on file. Surfaced behind the split-button chevron so
  // power users can subscribe to the complete archive instead of just
  // the recent slice.
  protected readonly subscribeFullUrl = computed(() => `${this.subscribeUrl()}?limit=false`);
  protected readonly copyHint = signal<'idle' | 'copied'>('idle');

  // Local draft mirrors the URL `q` on mount and is the source of truth
  // while the user is typing. Submit pushes it back into the URL.
  protected readonly searchDraft = signal('');

  // Palette extracted from the cover via node-vibrant. Pushed onto the global
  // --page-tint / --page-tint-bottom variables so the shell paints a faded
  // gradient across the whole content area (Spotify/Apple Music feel).
  private readonly palette = signal<CoverPalette | null>(null);

  // Per-button accent override. Material 19 uses per-component MDC tokens
  // (--mat-button-filled-container-color, --mat-fab-container-color, ...)
  // — overriding --mat-sys-primary on a parent isn't enough since those
  // tokens are resolved at theme-compile time. We set the relevant tokens
  // inline so flat-button and fab variants pick up the cover accent. Null
  // when no palette → Angular removes the styles and theme defaults apply.
  protected readonly actionStyles = computed(() => {
    const p = this.palette();
    const primary = p?.vibrant?.hex ?? p?.darkVibrant?.hex;
    const onPrimary = p?.vibrant?.titleText ?? p?.darkVibrant?.titleText;
    if (!primary || !onPrimary) return null;
    return {
      '--mat-button-filled-container-color': primary,
      '--mat-button-filled-label-text-color': onPrimary,
      '--mat-fab-container-color': primary,
      '--mat-fab-foreground-color': onPrimary,
      '--mat-sys-primary': primary,
      '--mat-sys-on-primary': onPrimary,
    };
  });

  // Sticky-bar visibility: true once the user has scrolled past the hero's
  // full natural height. Drives the `<ps-detail-sticky-header>`. Same
  // pattern as `/playlists/:id`: full hero stays statically in flow (no
  // class changes during scroll), a separate compact bar fades in on top.
  // The two-element design avoids any layout work mid-scroll on iOS
  // Safari which was freezing momentum scroll on iPad.
  private readonly heroSentinel = viewChild<ElementRef<HTMLElement>>('heroSentinel');
  protected readonly heroOffscreen = signal(false);

  constructor() {
    // Mirror the URL `?q=` into the search input — on first mount, and
    // again whenever the URL changes (e.g. arriving with a `?q=` deep
    // link). The user typing changes `searchDraft` but not `q()`, so this
    // effect doesn't fight the input — it only runs when the URL is the
    // source of change.
    effect(() => this.searchDraft.set(this.q()));

    // This route is detached rather than destroyed, so the panel has to be
    // closed from the URL — otherwise it would still be over the next page.
    effect(() => {
      const path = this.currentUrl.url().split('?')[0];
      if (!path.startsWith('/podcasts/')) this.panelOpen.set(false);
    });

    // The CDK places an anchored overlay once and then only follows scrolling,
    // not the page reflowing underneath it.
    effect(() => {
      this.status();
      this.itemsQuery.data();
      if (!this.panelOpen()) return;
      this.filtersOverlay()?.overlayRef?.updatePosition();
    });

    effect(() => {
      const url = this.coverSrc();
      if (!url) return;
      this.coverColor.extract(url).then((p) => this.palette.set(p));
    });

    // Claim the tint for the URL we are rendered at. PageTintService only
    // applies it while that URL stays active, so no destroy-time cleanup is
    // needed — which matters now that ListRouteReuseStrategy detaches this
    // page instead of destroying it. Reading `currentUrl` re-runs the
    // effect on re-attach (browser back), re-claiming the tint.
    effect(() => {
      const p = this.palette();
      const url = this.pageTint.currentUrl();
      if (!this.hostElement.nativeElement.isConnected) return;
      this.pageTint.claim(url, p);
    });

    // Watch the hero sentinel from the scrollable shell outlet's viewport.
    // Once the sentinel exits view (i.e. the hero is fully scrolled out),
    // toggle `heroOffscreen` to reveal the sticky compact header.
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

  // Per-item action list built fresh for each row so Reset only appears when
  // there's a file on disk to reset, and "Open original" carries the item's
  // remote URL into the menu entry (rendered as an external link).
  protected itemActions(item: ItemHAL): readonly CoverCardMenuEntry[] {
    // Three semantic sections joined with `MENU_DIVIDER` between non-
    // empty ones (see joinSections). Order chosen for predictability
    // across the whole app — Library, PodcastDetail and PlaylistDetail
    // all follow it:
    //   1. Open (single entry or submenu) + Add to playlist
    //   2. Queue affordances, reactive on player signals
    //   3. Reset (downloaded only) + Delete
    const header: CoverCardMenuEntry[] = [];
    const openItems: CoverCardAction[] = [
      {
        id: 'open-original',
        label: 'Open original URL',
        icon: 'language',
        url: item.url,
      },
    ];
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
    } else {
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
    danger.push(DELETE_ITEM_ACTION);

    return joinSections(header, queue, danger);
  }

  protected subtitleFor(item: ItemHAL): string {
    if (!item.pubDate) return 'No date';
    return new Date(item.pubDate).toLocaleDateString(undefined, {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
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

  protected onSubmitSearch() {
    const q = this.searchDraft().trim();
    // Reset to first page on new query — otherwise we'd land on a stale page
    // index that may be out of range for the filtered result set.
    this.router.navigate([], {
      queryParams: { q: q || null, page: 0 },
      queryParamsHandling: 'merge',
    });
  }

  protected onClearSearch() {
    this.searchDraft.set('');
    this.router.navigate([], {
      queryParams: { q: null, page: 0 },
      queryParamsHandling: 'merge',
    });
  }

  protected readonly panelOpen = signal(false);
  protected readonly hasActiveFilters = computed(() => this.status() !== 'all');
  protected readonly activeFilterCount = computed(() => (this.hasActiveFilters() ? 1 : 0));
  protected readonly statusFilterLabel = computed(
    () => STATUS_FILTER_OPTIONS.find((o) => o.value === this.status())?.label ?? '',
  );

  protected onTogglePanel() {
    this.panelOpen.update((open) => !open);
  }

  /** Focus returns to the chevron only once the overlay has actually gone. */
  protected onClosePanel() {
    this.panelOpen.set(false);
  }

  protected onPanelDetached() {
    this.panelOpen.set(false);
    const trigger = this.filtersTrigger()?.nativeElement;
    if (trigger?.isConnected) trigger.focus();
  }

  /** Filters apply as they are set, so the panel stays open. */
  protected onFiltersChanged(filters: ItemFilters) {
    this.navigateToStatus(filters.status);
  }

  protected onRemoveStatusFilter() {
    this.navigateToStatus('all');
  }

  private navigateToStatus(status: StatusFilter) {
    this.router.navigate([], {
      queryParams: { status: status === 'all' ? null : status, page: 0 },
      queryParamsHandling: 'merge',
    });
  }

  protected onPlay(item: ItemHAL) {
    this.player.open(item);
  }

  protected onDownload(item: ItemHAL) {
    this.itemApi.triggerDownload(item.podcastId, item.id).subscribe();
  }

  protected onOpenItem(item: ItemHAL) {
    this.navOrigin.set('podcast');
    this.router.navigate(['/podcasts', item.podcastId, 'items', item.id]);
  }

  protected onItemAction(item: ItemHAL, action: CoverCardAction) {
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
          data: { itemId: item.id, itemTitle: item.title },
          autoFocus: 'first-tabbable',
          panelClass: 'ps-fitting-dialog',
        });
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
      // "open-original" is a link (CoverCardAction.url) — handled by the
      // browser directly, no callback needed.
    }
  }

  private onResetItem(item: ItemHAL) {
    this.resetItemMutation.mutate(
      { podcastId: item.podcastId, itemId: item.id },
      {
        onSuccess: () => {
          this.player.closeIf(item.id);
          this.snackbar.open('Item reset', undefined, { duration: 2500 });
        },
        onError: () => this.snackbar.open('Could not reset item', 'Dismiss', { duration: 4000 }),
      },
    );
  }

  private onDeleteItem(item: ItemHAL) {
    if (!confirm(`Delete "${item.title}"?`)) return;
    this.deleteItemMutation.mutate(
      { podcastId: item.podcastId, itemId: item.id },
      {
        onSuccess: () => {
          this.player.closeIf(item.id);
          this.snackbar.open('Item deleted', undefined, { duration: 2500 });
        },
        onError: () => this.snackbar.open('Could not delete item', 'Dismiss', { duration: 4000 }),
      },
    );
  }

  protected onCopyRss() {
    const url = this.rssUrl();
    navigator.clipboard.writeText(url).then(
      () => {
        this.copyHint.set('copied');
        this.snackbar.open('RSS URL copied', undefined, { duration: 2500 });
        setTimeout(() => this.copyHint.set('idle'), 2500);
      },
      () => this.snackbar.open('Could not copy the URL', 'Dismiss', { duration: 4000 }),
    );
  }

  protected onUpdateNow(podcast: PodcastHAL) {
    this.api.triggerUpdate(podcast.id).subscribe({
      next: () => this.snackbar.open('Update started', undefined, { duration: 2500 }),
      error: () => this.snackbar.open('Could not start update', 'Dismiss', { duration: 4000 }),
    });
  }

  protected onUpdateAndDownload(podcast: PodcastHAL) {
    this.api.triggerUpdate(podcast.id, { download: true }).subscribe({
      next: () =>
        this.snackbar.open('Update started — new episodes will be downloaded', undefined, {
          duration: 3000,
        }),
      error: () => this.snackbar.open('Could not start update', 'Dismiss', { duration: 4000 }),
    });
  }

  protected onOpenSettings(podcast: PodcastHAL) {
    this.dialog
      .open(PodcastEditDialogComponent, {
        data: podcast,
        autoFocus: 'first-tabbable',
        panelClass: 'ps-fitting-dialog',
      })
      .afterClosed()
      .subscribe((saved) => {
        if (saved) {
          this.queryClient.invalidateQueries({
            queryKey: queryKeys.podcasts.detail(this.idPodcast()),
          });
          this.coverVersion.update((n) => n + 1);
        }
      });
  }

  protected onOpenUpload(podcast: PodcastHAL) {
    this.dialog
      .open(PodcastUploadDialogComponent, {
        data: podcast.id,
        autoFocus: 'first-tabbable',
        panelClass: 'ps-fitting-dialog',
      })
      .afterClosed()
      .subscribe((uploaded) => {
        // Upload dialog handles its own mutation; we just need to refresh the
        // items grid for THIS podcast since the dialog only knows it changed.
        if (uploaded) {
          this.queryClient.invalidateQueries({
            queryKey: queryKeys.podcasts.items(this.itemsInput()),
          });
        }
      });
  }

  protected onDelete(podcast: PodcastHAL) {
    if (!confirm(`Delete "${podcast.title}" and all its episodes?`)) return;
    this.deleteMutation.mutate(podcast.id, {
      onSuccess: () => {
        this.snackbar.open('Podcast deleted', undefined, { duration: 2500 });
        this.router.navigate(['/podcasts']);
      },
      onError: () =>
        this.snackbar.open('Could not delete the podcast', 'Dismiss', { duration: 4000 }),
    });
  }
}
