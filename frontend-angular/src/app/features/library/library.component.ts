import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  computed,
  effect,
  inject,
  input,
  signal,
  viewChild,
} from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CdkConnectedOverlay, OverlayModule } from '@angular/cdk/overlay';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatBadgeModule } from '@angular/material/badge';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import {
  CoverCardAction,
  CoverCardComponent,
  CoverCardMenuEntry,
  joinSections,
} from '../../shared/cover-card/cover-card.component';
import { PagerComponent } from '../../shared/pager/pager.component';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import {
  StatusBadgeComponent,
  StatusBadgeKind,
} from '../../shared/status-badge/status-badge.component';
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
import { ItemApi, ItemSearchInput } from '../../core/api/item.api';
import { ItemHAL } from '../../core/models/item.model';
import { TagInput } from '../../core/models/tag.model';
import { DownloadStreamService } from '../../core/downloads/download-stream.service';
import { PlayerService } from '../../core/player/player.service';
import { CurrentUrlService } from '../../core/navigation/current-url.service';
import { NavigationOriginService } from '../../core/navigation/navigation-origin.service';
import { OPEN_IN_VLC_ACTION, VlcService } from '../../core/vlc/vlc.service';
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
  selector: 'ps-library',
  standalone: true,
  imports: [
    FormsModule,
    OverlayModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatButtonModule,
    MatBadgeModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    CoverCardComponent,
    PagerComponent,
    EmptyStateComponent,
    StatusBadgeComponent,
    ItemFiltersPanelComponent,
  ],
  templateUrl: './library.component.html',
  styleUrl: './library.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class LibraryComponent {
  // Bound to /library?q=&page=&sort=&tags=&status= via withComponentInputBinding().
  readonly q = input<string>('');
  readonly page = input<number, string | number>(0, {
    transform: (v) => {
      const n = typeof v === 'number' ? v : Number.parseInt(v, 10);
      return Number.isFinite(n) && n >= 0 ? n : 0;
    },
  });
  readonly sort = input<string>('pubDate,DESC');
  // `withComponentInputBinding` sets *every* input on each navigation, passing
  // `undefined` for a query param that is absent, so both transforms below have
  // to accept it — as `page`'s already does. A transform that throws aborts the
  // whole binding loop, silently leaving the inputs declared after it unset.
  readonly tags = input<readonly string[], string | undefined>([], {
    transform: (v) =>
      (v ?? '')
        .split(',')
        .map((t) => t.trim())
        .filter((t) => t.length > 0),
  });
  /**
   * The URL carries the app's three-way vocabulary (`?status=not-downloaded`),
   * never the API's status list. A shared link therefore stays valid even if
   * the backend enum changes; `searchInput` expands it at the last moment.
   */
  readonly status = input<StatusFilter, string | undefined>('all', {
    transform: (v) => (isStatusFilter(v) ? v : 'all'),
  });

  private readonly router = inject(Router);
  private readonly itemApi = inject(ItemApi);
  private readonly stream = inject(DownloadStreamService);
  protected readonly player = inject(PlayerService);
  private readonly dialog = inject(MatDialog);
  private readonly snackbar = inject(MatSnackBar);
  private readonly resetItemMutation = this.itemApi.resetMutation();
  private readonly deleteItemMutation = this.itemApi.deleteMutation();
  private readonly navOrigin = inject(NavigationOriginService);
  private readonly currentUrl = inject(CurrentUrlService);
  private readonly vlc = inject(VlcService);

  protected actionsFor(item: ItemHAL): readonly CoverCardMenuEntry[] {
    // Menu is laid out as three sections, joined by `MENU_DIVIDER` only
    // between non-empty ones so we never end up with a dangling line:
    //   1. Open (single entry or submenu) + Add to playlist
    //   2. Queue: Play next / Add to queue / Remove from queue
    //      (reactive on `player` signals; absent for the currently-
    //      playing item or for items the player can't reach because
    //      they're not yet downloaded)
    //   3. Reset (downloaded only) + Delete
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
    danger.push(DELETE_ITEM_ACTION);

    return joinSections(header, queue, danger);
  }

  protected readonly searchDraft = signal('');
  protected readonly panelOpen = signal(false);

  // Read as ElementRef, not as the MatIconButton the ref would resolve to by
  // default, since all we need is to put focus back on the chevron.
  private readonly filtersTrigger = viewChild('filtersTrigger', {
    read: ElementRef<HTMLElement>,
  });
  private readonly filtersOverlay = viewChild(CdkConnectedOverlay);

  protected readonly searchInput = computed<ItemSearchInput>(() => ({
    q: this.q(),
    page: this.page(),
    size: DEFAULT_PAGE_SIZE,
    sort: this.sort(),
    tags: this.tags(),
    // `all` expands to an empty list, and ItemApi.search omits empty lists, so
    // no `status` parameter is sent at all.
    status: statusesFor(this.status()),
  }));

  protected readonly itemsQuery = this.itemApi.search(this.searchInput);

  /** The panel takes tag objects; the URL only ever carries names. */
  protected readonly tagFilters = computed<readonly TagInput[]>(() =>
    this.tags().map((name) => ({ name })),
  );

  protected readonly activeFilterCount = computed(
    () => this.tags().length + (this.status() === 'all' ? 0 : 1),
  );
  protected readonly hasActiveFilters = computed(() => this.activeFilterCount() > 0);

  protected readonly statusFilterLabel = computed(
    () => STATUS_FILTER_OPTIONS.find((o) => o.value === this.status())?.label ?? '',
  );

  constructor() {
    // Mirror the URL into the search input on first load / direct nav.
    this.searchDraft.set(this.q());

    // The overlay used to be disposed on navigation, which is no longer usable:
    // applying a filter *is* a navigation, and it would close the panel on every
    // click. So close it only when we genuinely leave the page. This has to be
    // driven by the URL rather than a lifecycle hook, because
    // ListRouteReuseStrategy detaches /library instead of destroying it — see
    // CurrentUrlService, which exists for exactly that.
    effect(() => {
      const path = this.currentUrl.url().split('?')[0];
      if (path !== '/library') this.panelOpen.set(false);
    });

    // The CDK places an anchored overlay once and then only follows scrolling,
    // not the page reflowing underneath it. Filtering changes how much content
    // the grid holds, which can move the field sideways, and the panel would
    // stay where it was — visibly unstuck from the field it belongs to.
    // `scrollbar-gutter` removes the usual cause; this covers the rest.
    effect(() => {
      this.tags();
      this.status();
      this.itemsQuery.data();
      if (!this.panelOpen()) return;
      this.filtersOverlay()?.overlayRef?.updatePosition();
    });
  }

  protected onSubmitSearch() {
    const q = this.searchDraft().trim();
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

  protected onTogglePanel() {
    this.panelOpen.update((open) => !open);
  }

  /**
   * Closes without committing. The overlay's template portal is destroyed with
   * it, which is what throws the pending draft away. Focus comes back to the
   * chevron in `onPanelDetached`, not here: removing the popover element moves
   * focus to `<body>` on its way out, which would undo a focus() called before
   * the detach.
   */
  protected onClosePanel() {
    this.panelOpen.set(false);
  }

  protected onPanelDetached() {
    this.panelOpen.set(false);
    const trigger = this.filtersTrigger()?.nativeElement;
    // Detach also fires when navigation disposes the overlay, where stealing
    // focus onto a chevron that is on its way out would be wrong.
    if (trigger?.isConnected) trigger.focus();
  }

  /**
   * Filters apply as they are set, so the panel stays open: the point of live
   * filtering is watching the grid answer while you tune it.
   */
  protected onFiltersChanged(filters: ItemFilters) {
    this.navigateToFilters(
      filters.tags.map((t) => t.name),
      filters.status,
    );
  }

  /** Removing a chip is unambiguous, so it applies immediately. */
  protected onRemoveTagFilter(name: string) {
    this.navigateToFilters(
      this.tags().filter((t) => t !== name),
      this.status(),
    );
  }

  protected onRemoveStatusFilter() {
    this.navigateToFilters(this.tags(), 'all');
  }

  protected onClearFilters() {
    this.navigateToFilters([], 'all');
  }

  private navigateToFilters(tags: readonly string[], status: StatusFilter) {
    this.router.navigate([], {
      queryParams: {
        // `null` drops the parameter entirely — never `tags=`.
        tags: tags.length > 0 ? tags.join(',') : null,
        status: status === 'all' ? null : status,
        // A narrower result set makes the current page number meaningless,
        // exactly as for a new text query.
        page: 0,
      },
      queryParamsHandling: 'merge',
    });
  }

  protected coverUrl(item: ItemHAL): string {
    return item.cover.proxyURL;
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
    this.player.open(item);
  }

  protected onDownload(item: ItemHAL) {
    this.itemApi.triggerDownload(item.podcastId, item.id).subscribe();
  }

  protected onOpen(item: ItemHAL) {
    // Mark this navigation's origin so item-detail scopes the
    // view-transition-name on the hero cover to morph only with this list.
    this.navOrigin.set('library');
    this.router.navigate(['/podcasts', item.podcastId, 'items', item.id]);
  }

  protected onAction(item: ItemHAL, action: CoverCardAction) {
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
          data: { itemId: item.id, itemTitle: item.title, podcastId: item.podcastId },
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
}
