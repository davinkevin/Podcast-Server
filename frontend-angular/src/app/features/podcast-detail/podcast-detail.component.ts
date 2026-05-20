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
import { QueryClient } from '@tanstack/angular-query-experimental';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import {
  CoverCardAction,
  CoverCardMenuEntry,
} from '../../shared/cover-card/cover-card.component';
import { TrackRowComponent } from '../../shared/track-row/track-row.component';
import { PagerComponent } from '../../shared/pager/pager.component';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
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
import {
  applyCoverTint,
  clearCoverTint,
  CoverColorService,
  CoverPalette,
} from '../../core/cover-color/cover-color.service';
import { SettingsService } from '../../core/settings/settings.service';
import { NavigationOriginService } from '../../core/navigation/navigation-origin.service';
import {
  OPEN_IN_VLC_ACTION,
  VlcService,
} from '../../core/vlc/vlc.service';
import { queryKeys } from '../../core/api/query-keys';

import { PodcastEditDialogComponent } from './podcast-edit-dialog.component';
import { PodcastUploadDialogComponent } from './podcast-upload-dialog.component';
import { AddToPlaylistDialogComponent } from '../playlists/add-to-playlist-dialog.component';

const DEFAULT_PAGE_SIZE = 24;
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
    StatusBadgeComponent,
    DetailStickyHeaderComponent,
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

  private readonly router = inject(Router);
  private readonly api = inject(PodcastApi);
  private readonly itemApi = inject(ItemApi);
  private readonly stream = inject(DownloadStreamService);
  private readonly player = inject(PlayerService);
  private readonly dialog = inject(MatDialog);
  private readonly snackbar = inject(MatSnackBar);
  private readonly coverColor = inject(CoverColorService);
  private readonly settings = inject(SettingsService);
  private readonly queryClient = inject(QueryClient);
  private readonly navOrigin = inject(NavigationOriginService);
  private readonly vlc = inject(VlcService);

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
  }));
  protected readonly itemsQuery = this.api.items(this.itemsInput);

  // Local draft mirrors the URL `q` on mount and is the source of truth
  // while the user is typing. Submit pushes it back into the URL.
  protected readonly searchDraft = signal('');

  // Palette extracted from the cover via node-vibrant. Pushed onto the global
  // --page-tint / --page-tint-bottom variables so the shell paints a faded
  // gradient across the whole content area (Spotify/Apple Music feel).
  private readonly palette = signal<CoverPalette | null>(null);

  // Per-button accent override. Material 19 uses per-component MDC tokens
  // (--mdc-filled-button-container-color, --mdc-fab-container-color, ...)
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
    // Mirror the URL `?q=` into the search input — on first mount, and
    // again whenever the URL changes (e.g. browser back from item-detail:
    // ListRouteReuseStrategy doesn't retain podcast-detail, so the
    // component remounts and the input field would otherwise be empty
    // even though the URL still carries the query). The user typing
    // changes `searchDraft` but not `q()`, so this effect doesn't fight
    // the input — it only runs when the URL is the source of change.
    effect(() => this.searchDraft.set(this.q()));

    effect(() => {
      const url = this.coverSrc();
      if (!url) return;
      this.coverColor.extract(url).then((p) => this.palette.set(p));
    });

    effect((onCleanup) => {
      const p = this.palette();
      const dark = this.settings.effectiveTheme() === 'dark';
      applyCoverTint(p, dark);
      onCleanup(() => clearCoverTint());
    });

    // Watch the hero sentinel from the scrollable shell outlet's viewport.
    // The sentinel sits at the bottom of the hero; when it intersects the
    // viewport, the hero is visible → compact header hidden. When it
    // scrolls out → compact header shown.
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
    const entries: CoverCardMenuEntry[] = [ADD_TO_PLAYLIST_ACTION];
    // Fold every "open in X" verb together: Source (web) → Downloaded
    // file → VLC. Only the first is available when the item isn't yet
    // downloaded, in which case it goes at the top level rather than
    // hiding behind an "Open" submenu trigger that would only show one
    // child.
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
      entries.push(openItems[0]);
    } else {
      entries.push({
        kind: 'group',
        label: 'Open',
        icon: 'open_in_new',
        items: openItems,
      });
    }
    if (item.isDownloaded) entries.push(RESET_ITEM_ACTION);
    entries.push(DELETE_ITEM_ACTION);
    return entries;
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

  protected onUpdateNow(podcast: PodcastHAL) {
    this.api.triggerUpdate(podcast.id).subscribe({
      next: () =>
        this.snackbar.open('Update started', undefined, { duration: 2500 }),
      error: () =>
        this.snackbar.open('Could not start update', 'Dismiss', { duration: 4000 }),
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
