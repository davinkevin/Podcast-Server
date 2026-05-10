import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { CoverCardComponent } from '../../shared/cover-card/cover-card.component';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { PodcastApi } from '../../core/api/podcast.api';
import { PodcastHAL } from '../../core/models/podcast.model';

@Component({
  selector: 'ps-podcasts',
  standalone: true,
  imports: [MatProgressSpinnerModule, CoverCardComponent, EmptyStateComponent],
  templateUrl: './podcasts.component.html',
  styleUrl: './podcasts.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class PodcastsComponent {
  private readonly router = inject(Router);
  private readonly api = inject(PodcastApi);

  protected readonly podcastsResource = this.api.list();

  protected coverUrl(p: PodcastHAL): string {
    return p.cover.url;
  }

  protected onOpen(p: PodcastHAL) {
    this.router.navigate(['/podcasts', p.id]);
  }
}
