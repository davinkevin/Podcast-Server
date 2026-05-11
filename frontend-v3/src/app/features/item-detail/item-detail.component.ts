import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  signal,
} from '@angular/core';
import { Router } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { DatePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';

import { ItemApi, ItemRef } from '../../core/api/item.api';
import { ItemHAL } from '../../core/models/item.model';
import { PlayerService } from '../../core/player/player.service';
import { DownloadStreamService } from '../../core/downloads/download-stream.service';
import {
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
    DatePipe,
    MatIconModule,
    MatButtonModule,
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
  protected readonly itemResource = this.itemApi.getById(this.ref);

  // Palette from the item's cover, propagated via --page-tint / --page-tint-
  // bottom so the whole content area picks up the color like the podcast page.
  private readonly palette = signal<CoverPalette | null>(null);

  constructor() {
    effect(() => {
      const item = this.itemResource.value();
      if (item) this.title.setTitle(`${item.title} — Podcast Server`);
    });

    effect(() => {
      const item = this.itemResource.value();
      if (!item) return;
      this.coverColor.extract(item.cover.url).then((p) => this.palette.set(p));
    });

    effect((onCleanup) => {
      const p = this.palette();
      const dark = this.settings.effectiveTheme() === 'dark';
      const top = dark
        ? p?.darkVibrant ?? p?.vibrant
        : p?.vibrant ?? p?.lightVibrant;
      const bottom = dark
        ? p?.darkMuted ?? p?.muted
        : p?.muted ?? p?.lightMuted;
      const root = document.documentElement;
      if (top) root.style.setProperty('--page-tint', top);
      else root.style.removeProperty('--page-tint');
      if (bottom) root.style.setProperty('--page-tint-bottom', bottom);
      else root.style.removeProperty('--page-tint-bottom');
      onCleanup(() => {
        root.style.removeProperty('--page-tint');
        root.style.removeProperty('--page-tint-bottom');
      });
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
    this.itemApi.reset(item.podcastId, item.id).subscribe({
      next: () => {
        // Reset deletes the file on disk; if it's playing, the proxyURL would 404 — close.
        this.player.closeIf(item.id);
        this.snackbar.open('Item reset', undefined, { duration: 2500 });
        this.itemResource.reload();
      },
      error: () => this.snackbar.open('Could not reset item', 'Dismiss', { duration: 4000 }),
    });
  }

  protected onDelete(item: ItemHAL) {
    if (!confirm(`Delete "${item.title}"?`)) return;
    this.itemApi.delete(item.podcastId, item.id).subscribe({
      next: () => {
        this.player.closeIf(item.id);
        this.snackbar.open('Item deleted', undefined, { duration: 2500 });
        this.router.navigate(['/library']);
      },
      error: () => this.snackbar.open('Could not delete item', 'Dismiss', { duration: 4000 }),
    });
  }

  protected onAddToPlaylist(item: ItemHAL) {
    this.dialog.open(AddToPlaylistDialogComponent, {
      data: { itemId: item.id, itemTitle: item.title, podcastId: item.podcastId },
      autoFocus: 'first-tabbable',
      panelClass: 'ps-fitting-dialog',
    });
  }
}
