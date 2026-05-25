import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  signal,
} from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
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
import { ItemApi, ItemSearchInput } from '../../core/api/item.api';
import { ItemHAL } from '../../core/models/item.model';
import { DownloadStreamService } from '../../core/downloads/download-stream.service';
import { PlayerService } from '../../core/player/player.service';
import { NavigationOriginService } from '../../core/navigation/navigation-origin.service';
import {
  OPEN_IN_VLC_ACTION,
  VlcService,
} from '../../core/vlc/vlc.service';
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
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    CoverCardComponent,
    PagerComponent,
    EmptyStateComponent,
    StatusBadgeComponent,
  ],
  templateUrl: './library.component.html',
  styleUrl: './library.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class LibraryComponent {
  // Bound to /library?q=&page=&sort= via withComponentInputBinding().
  readonly q = input<string>('');
  readonly page = input<number, string | number>(0, {
    transform: (v) => {
      const n = typeof v === 'number' ? v : Number.parseInt(v, 10);
      return Number.isFinite(n) && n >= 0 ? n : 0;
    },
  });
  readonly sort = input<string>('pubDate,DESC');

  private readonly router = inject(Router);
  private readonly itemApi = inject(ItemApi);
  private readonly stream = inject(DownloadStreamService);
  protected readonly player = inject(PlayerService);
  private readonly dialog = inject(MatDialog);
  private readonly snackbar = inject(MatSnackBar);
  private readonly resetItemMutation = this.itemApi.resetMutation();
  private readonly deleteItemMutation = this.itemApi.deleteMutation();
  private readonly navOrigin = inject(NavigationOriginService);
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

  protected readonly searchInput = computed<ItemSearchInput>(() => ({
    q: this.q(),
    page: this.page(),
    size: DEFAULT_PAGE_SIZE,
    sort: this.sort(),
  }));

  protected readonly itemsQuery = this.itemApi.search(this.searchInput);

  constructor() {
    // Mirror the URL into the search input on first load / direct nav.
    this.searchDraft.set(this.q());
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

  protected coverUrl(item: ItemHAL): string {
    return item.cover.url;
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
        onError: () =>
          this.snackbar.open('Could not reset item', 'Dismiss', { duration: 4000 }),
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
        onError: () =>
          this.snackbar.open('Could not delete item', 'Dismiss', { duration: 4000 }),
      },
    );
  }
}
