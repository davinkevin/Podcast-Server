import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
} from '@angular/core';
import { Router } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { DatePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { ItemApi, ItemRef } from '../../core/api/item.api';
import { ItemHAL } from '../../core/models/item.model';
import { PlayerService } from '../../core/player/player.service';
import { DownloadStreamService } from '../../core/downloads/download-stream.service';
import {
  StatusBadgeComponent,
  StatusBadgeKind,
} from '../../shared/status-badge/status-badge.component';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';

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

  protected readonly ref = computed<ItemRef>(() => ({
    podcastId: this.idPodcast(),
    id: this.id(),
  }));
  protected readonly itemResource = this.itemApi.getById(this.ref);

  constructor() {
    effect(() => {
      const item = this.itemResource.value();
      if (item) this.title.setTitle(`${item.title} — Podcast Server`);
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
}
