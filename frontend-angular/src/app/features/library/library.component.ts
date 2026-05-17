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

import {
  CoverCardAction,
  CoverCardComponent,
  CoverCardMenuEntry,
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
const ADD_TO_PLAYLIST_ACTION: CoverCardAction = {
  id: 'add-to-playlist',
  label: 'Add to playlist',
  icon: 'playlist_add',
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
  private readonly player = inject(PlayerService);
  private readonly dialog = inject(MatDialog);
  private readonly navOrigin = inject(NavigationOriginService);
  private readonly vlc = inject(VlcService);

  protected actionsFor(item: ItemHAL): readonly CoverCardMenuEntry[] {
    const entries: CoverCardMenuEntry[] = [ADD_TO_PLAYLIST_ACTION];
    // "Open" submenu — same shape as podcast-detail / playlist-detail.
    // Source URL always available when the item has one, the downloaded
    // variants only when the proxy URL is backed by a file on disk.
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
    if (openItems.length > 0) {
      entries.push({
        kind: 'group',
        label: 'Open',
        icon: 'open_in_new',
        items: openItems,
      });
    }
    return entries;
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
    }
  }
}
