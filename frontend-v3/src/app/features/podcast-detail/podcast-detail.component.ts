import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
} from '@angular/core';
import { Router } from '@angular/router';
import { DatePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { CoverCardComponent } from '../../shared/cover-card/cover-card.component';
import { PagerComponent } from '../../shared/pager/pager.component';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
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

import { PodcastEditDialogComponent } from './podcast-edit-dialog.component';
import { PodcastUploadDialogComponent } from './podcast-upload-dialog.component';

const DEFAULT_PAGE_SIZE = 24;

@Component({
  selector: 'ps-podcast-detail',
  standalone: true,
  imports: [
    DatePipe,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatProgressSpinnerModule,
    CoverCardComponent,
    PagerComponent,
    EmptyStateComponent,
    StatusBadgeComponent,
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

  private readonly router = inject(Router);
  private readonly api = inject(PodcastApi);
  private readonly itemApi = inject(ItemApi);
  private readonly stream = inject(DownloadStreamService);
  private readonly player = inject(PlayerService);
  private readonly dialog = inject(MatDialog);
  private readonly snackbar = inject(MatSnackBar);

  protected readonly id = computed(() => this.idPodcast());
  protected readonly podcastResource = this.api.getById(this.id);

  protected readonly itemsInput = computed<PodcastItemsInput>(() => ({
    podcastId: this.idPodcast(),
    page: this.page(),
    size: DEFAULT_PAGE_SIZE,
  }));
  protected readonly itemsResource = this.api.items(this.itemsInput);

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

  protected onOpenItem(item: ItemHAL) {
    this.router.navigate(['/podcasts', item.podcastId, 'items', item.id]);
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
      .open(PodcastEditDialogComponent, { data: podcast, autoFocus: 'first-tabbable' })
      .afterClosed()
      .subscribe((saved) => {
        if (saved) this.podcastResource.reload();
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
        if (uploaded) this.itemsResource.reload();
      });
  }

  protected onDelete(podcast: PodcastHAL) {
    if (!confirm(`Delete "${podcast.title}" and all its episodes?`)) return;
    this.api.delete(podcast.id).subscribe({
      next: () => {
        this.snackbar.open('Podcast deleted', undefined, { duration: 2500 });
        this.router.navigate(['/podcasts']);
      },
      error: () =>
        this.snackbar.open('Could not delete the podcast', 'Dismiss', { duration: 4000 }),
    });
  }
}
