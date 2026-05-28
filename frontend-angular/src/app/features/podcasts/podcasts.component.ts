import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
} from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDialog } from '@angular/material/dialog';
import { MatMenuModule } from '@angular/material/menu';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { QueryClient } from '@tanstack/angular-query-experimental';

import { CoverCardComponent } from '../../shared/cover-card/cover-card.component';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { PodcastApi } from '../../core/api/podcast.api';
import { PodcastHAL } from '../../core/models/podcast.model';
import { queryKeys } from '../../core/api/query-keys';
import { DownloadStreamService } from '../../core/downloads/download-stream.service';
import { PodcastCreateDialogComponent } from './podcast-create-dialog.component';
import { PodcastsListStateService } from './podcasts-list-state.service';

@Component({
  selector: 'ps-podcasts',
  standalone: true,
  imports: [
    FormsModule,
    MatProgressSpinnerModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatMenuModule,
    MatTooltipModule,
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
  private readonly state = inject(PodcastsListStateService);
  private readonly stream = inject(DownloadStreamService);
  private readonly snackbar = inject(MatSnackBar);

  // Bulk-refresh button state: spin + disable while SSE reports a global
  // update is in flight (driven by UpdateService.updateAll on the backend).
  protected readonly isUpdatingAll = this.stream.updating;

  // TanStack Query provides stale-while-revalidate out of the box — on return
  // navigation the cached list renders instantly while a background refetch
  // happens. No more PageCache + computed fallback boilerplate.
  protected readonly podcastsQuery = this.api.list();

  // Client-side search. The /api/v1/podcasts endpoint returns everything in
  // one shot so filtering by title is purely UI — no refetch on keystroke.
  // Backed by a session-scoped service so the filter survives navigation away
  // and back to this route.
  protected readonly search = this.state.search;

  // Backend orders by id; sort by lastUpdate DESC like v1 (nulls last), then
  // apply the search filter on top.
  protected readonly visiblePodcasts = computed<readonly PodcastHAL[]>(() => {
    const data = this.podcastsQuery.data();
    if (!data) return [];
    const sorted = [...data.content].sort((a, b) => {
      const at = a.lastUpdate ? Date.parse(a.lastUpdate) : Number.NEGATIVE_INFINITY;
      const bt = b.lastUpdate ? Date.parse(b.lastUpdate) : Number.NEGATIVE_INFINITY;
      return bt - at;
    });
    const q = this.search().trim().toLowerCase();
    if (!q) return sorted;
    return sorted.filter((p) => p.title.toLowerCase().includes(q));
  });

  protected coverUrl(p: PodcastHAL): string {
    return p.cover.proxyURL;
  }

  protected onOpen(p: PodcastHAL) {
    this.router.navigate(['/podcasts', p.id]);
  }

  protected onClearSearch() {
    this.search.set('');
  }

  protected onUpdateAll(withDownload: boolean) {
    if (this.isUpdatingAll()) return;
    this.api.updateAll({ download: withDownload }).subscribe({
      next: () =>
        this.snackbar.open(
          withDownload ? 'Update & download started' : 'Update started',
          undefined,
          { duration: 2500 },
        ),
      error: () =>
        this.snackbar.open('Could not start update', 'Dismiss', { duration: 4000 }),
    });
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
