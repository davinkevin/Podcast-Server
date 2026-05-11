import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { Router } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { QueryClient } from '@tanstack/angular-query-experimental';

import { CoverCardComponent } from '../../shared/cover-card/cover-card.component';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { PodcastApi } from '../../core/api/podcast.api';
import { PodcastHAL } from '../../core/models/podcast.model';
import { queryKeys } from '../../core/api/query-keys';
import { PodcastCreateDialogComponent } from './podcast-create-dialog.component';

@Component({
  selector: 'ps-podcasts',
  standalone: true,
  imports: [
    MatProgressSpinnerModule,
    MatButtonModule,
    MatIconModule,
    CoverCardComponent,
    EmptyStateComponent,
  ],
  templateUrl: './podcasts.component.html',
  styleUrl: './podcasts.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class PodcastsComponent {
  private readonly router = inject(Router);
  private readonly api = inject(PodcastApi);
  private readonly dialog = inject(MatDialog);
  private readonly queryClient = inject(QueryClient);

  // TanStack Query provides stale-while-revalidate out of the box — on return
  // navigation the cached list renders instantly while a background refetch
  // happens. No more PageCache + computed fallback boilerplate.
  protected readonly podcastsQuery = this.api.list();

  // Backend orders by id; sort by lastUpdate DESC like v1 (nulls last).
  protected readonly sortedPodcasts = computed<readonly PodcastHAL[]>(() => {
    const data = this.podcastsQuery.data();
    if (!data) return [];
    return [...data.content].sort((a, b) => {
      const at = a.lastUpdate ? Date.parse(a.lastUpdate) : Number.NEGATIVE_INFINITY;
      const bt = b.lastUpdate ? Date.parse(b.lastUpdate) : Number.NEGATIVE_INFINITY;
      return bt - at;
    });
  });

  protected coverUrl(p: PodcastHAL): string {
    return p.cover.url;
  }

  protected onOpen(p: PodcastHAL) {
    this.router.navigate(['/podcasts', p.id]);
  }

  protected onAdd() {
    this.dialog
      .open(PodcastCreateDialogComponent, {
        autoFocus: 'first-tabbable',
        panelClass: 'ps-fitting-dialog',
      })
      .afterClosed()
      .subscribe((created: PodcastHAL | undefined) => {
        if (!created) return;
        this.queryClient.invalidateQueries({
          queryKey: queryKeys.podcasts.list(),
        });
        this.router.navigate(['/podcasts', created.id]);
      });
  }
}
