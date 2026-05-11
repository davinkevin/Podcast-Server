import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
} from '@angular/core';
import { Router } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';

import { CoverCardComponent } from '../../shared/cover-card/cover-card.component';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { PodcastApi } from '../../core/api/podcast.api';
import { PodcastHAL, PodcastsContainerHAL } from '../../core/models/podcast.model';
import { PageCache } from '../../core/page-cache/page-cache.service';
import { PodcastCreateDialogComponent } from './podcast-create-dialog.component';

const CACHE_KEY = 'podcasts:list';

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
  private readonly pageCache = inject(PageCache);

  protected readonly podcastsResource = this.api.list();

  // Cached fallback so the grid is in DOM right at mount (return navigation)
  // — required for view-transition morphs from the detail page back to the
  // matching card to find a destination element at snapshot time.
  protected readonly podcastsResult = computed<PodcastsContainerHAL | undefined>(() => {
    const live = this.podcastsResource.value();
    if (live) return live;
    return this.pageCache.get<PodcastsContainerHAL>(CACHE_KEY);
  });

  constructor() {
    effect(() => {
      const value = this.podcastsResource.value();
      if (value) this.pageCache.put(CACHE_KEY, value);
    });
  }

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
        this.podcastsResource.reload();
        this.router.navigate(['/podcasts', created.id]);
      });
  }
}
