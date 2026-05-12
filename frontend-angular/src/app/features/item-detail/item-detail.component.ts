import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  signal,
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
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';

import { ItemApi, ItemRef } from '../../core/api/item.api';
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
  // Bound from /podcasts/:idPodcast/items/:id via withComponentInputBinding().
  readonly idPodcast = input.required<string>();
  readonly id = input.required<string>();

  // Origin of this navigation (set by the list that linked here). Consumed
  // once at construction so the view-transition-name on the hero cover is
  // scoped to that list only — avoids cross-list morphs that aren't a
  // natural navigation.
  private readonly origin: NavigationOrigin | null =
    inject(NavigationOriginService).consume();

  protected readonly heroTransitionName = computed<string | null>(() => {
    if (!this.origin) return null;
    return `${this.origin}-item-cover-${this.id()}`;
  });

  private readonly itemApi = inject(ItemApi);
  private readonly player = inject(PlayerService);
  private readonly stream = inject(DownloadStreamService);
  private readonly router = inject(Router);
  private readonly title = inject(Title);
  private readonly snackbar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  private readonly coverColor = inject(CoverColorService);
  private readonly settings = inject(SettingsService);

  protected readonly ref = computed<ItemRef>(() => ({
    podcastId: this.idPodcast(),
    id: this.id(),
  }));
  protected readonly itemQuery = this.itemApi.getById(this.ref);
  private readonly resetMutation = this.itemApi.resetMutation();
  private readonly deleteMutation = this.itemApi.deleteMutation();

  // Cover URL derived from the route so the cover img can render before the
  // item resource resolves — required for view-transition morphs from the
  // list page. Once the item arrives, swap to its real URL (handles non-jpg
  // covers gracefully).
  protected readonly coverSrc = computed(() => {
    const item = this.itemQuery.data();
    if (item) return item.cover.url;
    return `/api/v1/podcasts/${this.idPodcast()}/items/${this.id()}/cover.jpg`;
  });

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
      this.coverColor.extract(item.cover.url).then((p) => this.palette.set(p));
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
    this.player.open(item);
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

  protected onAddToPlaylist(item: ItemHAL) {
    this.dialog.open(AddToPlaylistDialogComponent, {
      data: { itemId: item.id, itemTitle: item.title, podcastId: item.podcastId },
      autoFocus: 'first-tabbable',
      panelClass: 'ps-fitting-dialog',
    });
  }
}
